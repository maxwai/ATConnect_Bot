package commands.event;

import bot.BotEvents;
import emoji.Emoji;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class Location {
	
	/**
	 * Hashmap with all active Messages to choose a position.
	 * <p>
	 * The Object array has following setup: {@code [Message, userID]}
	 */
	public final HashMap<Long, Object[]> messages = new HashMap<>();
	/**
	 * The Location Name
	 */
	public final String location;
	/**
	 * The List of positions
	 */
	public final ArrayList<String> positions;
	/**
	 * The Users at that Location
	 */
	public final ArrayList<Long> users = new ArrayList<>();
	/**
	 * The Positions of the Users at that Location. Same order as {@link #users}
	 */
	public final ArrayList<String> userPositions = new ArrayList<>();
	/**
	 * The List of reactions in the same order as {@link #positions}.
	 * <p>
	 * The Reactions have two possible Classes: {@link String} or {@link Emote}
	 */
	private ArrayList<Object> reactions;
	/**
	 * The Event Instance of that Location
	 */
	private EventInstance parent;
	
	/**
	 * This is the constructor for when an Location is retrieved by the XML Parser
	 *
	 * @param location The Location Title
	 * @param positions The Positions
	 * @param users The Users in the positions
	 * @param userPositions The Positions of the users
	 */
	public Location(@Nonnull String location, @Nonnull ArrayList<String> positions,
			@Nonnull ArrayList<Long> users, @Nonnull ArrayList<String> userPositions) {
		this.location = location;
		this.positions = positions;
		this.users.addAll(users);
		this.userPositions.addAll(userPositions);
	}
	
	
	/**
	 * Will create a Location with it's positions
	 *
	 * @param location The Name of the Location
	 * @param positions List of all Position Names
	 * @param guild The Guild of the Event
	 * @param parent The Event Instance calling this Constructor
	 */
	Location(@Nonnull String location, @Nonnull ArrayList<String> positions,
			@Nonnull Guild guild, @Nonnull EventInstance parent) {
		this.location = location;
		this.positions = positions;
		this.parent = parent;
		getReactions(guild);
	}
	
	/**
	 * Updates the Guild and Parent Instance. This method is exclusively used when the Location was
	 * created from the Config
	 *
	 * @param guild The Guild Instance
	 * @param parent The Parent Instance
	 */
	void updateGuildAndParent(@Nonnull Guild guild, @Nonnull EventInstance parent) {
		this.parent = parent;
		getReactions(guild);
	}
	
	/**
	 * Retrieves the needed Reactions for this Location
	 *
	 * @param guild The Guild of the Event
	 */
	private void getReactions(@Nonnull Guild guild) {
		reactions = new ArrayList<>(this.positions.size());
		for (int i = 0; i < this.positions.size(); i++) {
			List<Emote> emotes = guild.getEmotesByName(this.positions.get(i), true);
			if (emotes.size() == 0) {
				reactions.add(Emoji.numbersList.get(i));
			} else {
				reactions.add(emotes.get(0));
			}
		}
	}
	
	/**
	 * Returns the Title to be displayed in the Embed
	 *
	 * @return The String for the Title without Emoji yet
	 */
	@Nonnull
	String getTitle() {
		return location + " (" + users.size() + ")";
	}
	
	/**
	 * Return the Content for the Embed
	 *
	 * @param guild The Guild of the
	 *
	 * @return The String for the Content of a Location on the Event Embed
	 */
	@Nonnull
	String getContent(@Nonnull Guild guild) {
		if (users.size() == 0)
			return "-";
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < users.size(); i++) {
			content.append(userPositions.get(i)).append(" ")
					.append(BotEvents.getServerName(guild.getMemberById(users.get(i))))
					.append("\n");
		}
		return content.substring(0, content.length() - 1);
	}
	
	/**
	 * Adds a User to the Location at the specified Position if correct
	 *
	 * @param reactionEmote The Emote that was Reacted
	 * @param user The User that Reacted
	 * @param message The Message that was Reacted
	 *
	 * @return if the user was correctly added
	 */
	public boolean addUser(@Nonnull ReactionEmote reactionEmote, @Nonnull User user,
			@Nonnull Message message) {
		String reaction = reactionEmote.getAsReactionCode();
		if (reactionsContains(reaction)) {
			Long userID = user.getIdLong();
			if (messages.get(message.getIdLong())[1].equals(userID)) {
				parent.removeUser(userID);
				
				users.add(userID);
				if (reactionEmote.isEmoji())
					userPositions.add(positions.get(reactions.indexOf(reaction)) + ": ");
				else
					userPositions.add("<:" + reaction + ">");
				
				message.delete().queue();
				messages.remove(message.getIdLong());
				
				parent.updateEmbeds(false);
				return true;
			}
		}
		message.removeReaction(reaction, user).queue();
		return false;
	}
	
	/**
	 * Tests if the Reaction is contained in {@link #reactions}
	 *
	 * @param reaction The Reaction to Test
	 *
	 * @return if the reaction is in the List
	 */
	private boolean reactionsContains(@Nonnull String reaction) {
		for (Object react : reactions) {
			if (react instanceof Emote && ((Emote) react).getAsMention()
					.equals("<:" + reaction + ">"))
				return true;
			else if (react.equals(reaction))
				return true;
		}
		return false;
	}
	
	/**
	 * Deletes the User if he has a Role in this Location
	 *
	 * @param user The User to delete
	 */
	void deleteUser(@Nonnull Long user) {
		if (users.contains(user)) {
			userPositions.remove(users.indexOf(user));
			users.remove(user);
		}
	}
	
	/**
	 * Will send the User as a PM the Embed to choose his position in this Location
	 *
	 * @param channel The Channel where the Embed should be send
	 * @param user The User that made the request
	 */
	void postPositionEmbed(@Nonnull MessageChannel channel, @Nonnull User user) {
		EmbedBuilder eb = new EmbedBuilder();
		
		eb.setTitle("Choose your role " + user.getName() + " at " + location);
		
		StringBuilder positionString = new StringBuilder();
		
		for (int i = 0; i < positions.size(); i++) {
			if (reactions.get(i) instanceof Emote)
				positionString.append(((Emote) reactions.get(i)).getAsMention());
			else
				positionString.append((String) reactions.get(i));
			positionString.append(" - ").append(location).append(" ")
					.append(positions.get(i)).append("\n");
		}
		
		eb.addField("", positionString.substring(0, positionString.length() - 1), false);
		
		eb.addField("", "Press the reaction that fits your role!", false);
		
		eb.setFooter(
				"This message will be deleted in 60sec and you will not get signed up!");
		
		parent.locations.forEach(location -> location.deleteEmbedIfExist(user.getIdLong()));
		
		// send the message and add the Reactions for the positions
		channel.sendMessage(eb.build()).queue(message -> {
			for (Object reaction : reactions) {
				if (reaction instanceof Emote)
					message.addReaction((Emote) reaction).queue(unused -> {},
							new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
				else
					message.addReaction((String) reaction).queue(unused -> {},
							new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
			}
			messages.put(message.getIdLong(), new Object[]{message, user.getIdLong()});
			
			new Thread(() -> {
				try {
					Thread.sleep(60 * 1000);
				} catch (InterruptedException ignored) {
				}
				// delete the Message after X sec
				messages.remove(message.getIdLong());
				try {
					message.delete()
							.queue(unused -> {},
									new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
				} catch (RejectedExecutionException ignored) {
				}
			}).start();
		});
	}
	
	/**
	 * Deletes the Position choosing Embed for a User if it Exists
	 *
	 * @param user The User for which the Embed is
	 */
	private void deleteEmbedIfExist(Long user) {
		Set<Long> keys = messages.keySet();
		Long key = null;
		for (Long tempKey : keys) {
			Object[] array = messages.get(tempKey);
			if (array[1].equals(user)) {
				key = tempKey;
				break;
			}
		}
		if (key != null) {
			Object[] array = messages.get(key);
			((Message) array[0]).delete().queue();
			messages.remove(key);
		}
	}
}
