package bot;

import commands.BotStatus;
import commands.Countdowns;
import commands.Event;
import commands.Help;
import commands.Ping;
import commands.Purge;
import commands.Reload;
import commands.Timezones;
import commands.Trained;
import emoji.Emoji;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotEvents {
	
	private static final Logger logger = LoggerFactory.getLogger("BotStatus");
	
	
	/**
	 * Adds a Trashcan so that the Message can be easily deleted by users
	 *
	 * @param message The message where the Trashcan should be added
	 */
	public static void addTrashcan(Message message) {
		// add a reaction to make it easy to delete the post
		message.addReaction(Emoji.WASTEBASKET).queue();
	}
	
	public static void cannotSendPrivateMessage(@Nonnull MessageChannel channel,
			@Nonnull User user) {
		logger.warn("Could not send private message to " + user.getName());
		channel.sendMessage(user.getAsMention() + "couldn't send you a private message.")
				.queue(message -> BotEvents.deleteMessageAfterXTime(message, 10));
	}
	
	/**
	 * Deletes the message after the given amount of time in Seconds
	 *
	 * @param message The message to delete
	 * @param seconds Time until the message should be deleted
	 */
	public static void deleteMessageAfterXTime(Message message, long seconds) {
		new Thread(() -> {
			try {
				Thread.sleep(seconds * 1000);
			} catch (InterruptedException ignored) {
			}
			// delete the Message after X sec
			try {
				message.delete()
						.queue(unused -> {},
								new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
			} catch (RejectedExecutionException ignored) {
			}
		}).start();
	}
	
	/**
	 * Returns the Name of the User on the Server
	 *
	 * @param member User where we want the name
	 *
	 * @return Return the Nickname, or username if nickname is not available
	 */
	public static String getServerName(@Nullable Member member) {
		if (member == null)
			return "User-Not-Found";
		String nickname = member.getNickname();
		if (nickname == null)
			nickname = member.getEffectiveName();
		return nickname;
	}
	
	/**
	 * Is triggered when a Member leaves the Guild
	 *
	 * @param event Member leave Event
	 */
	@SubscribeEvent
	public void onMemberLeave(GuildMemberRemoveEvent event) {
		// only react it is from the setup Guild
		if (event.getGuild().getIdLong() == BotMain.ROLES.get("Guild")) {
			Member member = event.getMember();
			String nickname = null;
			if (member != null)
				nickname = member.getNickname();
			String userTag = event.getUser().getAsTag();
			String message;
			if (nickname == null)
				message = userTag + " left the Server";
			else
				message = nickname + " left the Server. User Tag: " + userTag;
			TextChannel channel = event.getGuild()
					.getTextChannelById(BotMain.ROLES.get("SystemLogs"));
			logger.warn(message);
			if (channel != null) {
				channel.sendMessage(message).queue();
			} else {
				logger.error("SystemLogs Channel ID not correct");
			}
		}
	}
	
	/**
	 * Is triggered when an Emote is added in a Private Chat (at least it is filtered that way)
	 *
	 * @param event Reaction add Event
	 */
	@SubscribeEvent
	public void onPrivateEmoteAdded(MessageReactionAddEvent event) {
		if (!event.isFromGuild()) {
			if (event.getUser() == null || event.getUser().isBot())
				return; // don't react if the bot is adding this emote
			event.retrieveMessage().queue(message -> {
				if (!message.getAuthor().isBot()) return; // delete only bot messages
				if (!Event.reactionAdded(message, event.getReactionEmote(), event.getUser(),
						event.getChannel())) {
					if (event.getReactionEmote().isEmoji()) {
						String emoji = Emoji.getCleanedUpEmoji(event.getReactionEmote().getEmoji());
						if (emoji.equals(Emoji.WASTEBASKET)) {
							LoggerFactory.getLogger("ReactionAdded")
									.info("deleting message because of :wastebasket: reaction");
							message.delete().queue(); // delete the message
						}
					}
				}
			});
		}
	}
	
	/**
	 * Is triggered when an Emote is added / count is changed
	 *
	 * @param event Reaction add Event
	 */
	@SubscribeEvent
	public void onGuildEmoteAdded(GuildMessageReactionAddEvent event) {
		if (event.getUser().isBot()) return; // don't react if the bot is adding this emote
		Member authorMember = event.getGuild().getMember(event.getUser());
		List<Role> rolesOfUser = authorMember != null ? authorMember.getRoles() : new ArrayList<>();
		boolean isAdmin =
				rolesOfUser.contains(event.getGuild().getRoleById(BotMain.ROLES.get("Admin"))) ||
						event.getUser().getIdLong() == BotMain.ROLES.get("Owner");
		event.retrieveMessage()
				.queue(message -> {
					if (!message.getAuthor().isBot()) return; // only bot messages
					// check if it is a reaction for an event
					if (!Event.reactionAdded(message, event.getReactionEmote(), event.getUser(),
							event.getChannel())) {
						// only do something if he is admin or the wastebasket was already here from the bot
						if ((isAdmin || message.getReactions().get(0).isSelf())
								&& event.getReactionEmote().isEmoji()) {
							String emoji = Emoji
									.getCleanedUpEmoji(event.getReactionEmote().getEmoji());
							if (emoji.equals(Emoji.WASTEBASKET)) {
								LoggerFactory.getLogger("ReactionAdded")
										.info("deleting message because of :wastebasket: reaction");
								// check if this message was part of a Countdown
								if (Countdowns.messageIds.contains(message.getId()))
									// close that countdown since the message will be deleted
									Countdowns.closeSpecificThread(message.getId());
								message.delete().queue(); // delete the message
							}
						}
					}
				});
	}
	
	/**
	 * Is triggered when the Nickname of a User is changed
	 *
	 * @param event Nickname Update Event
	 */
	@SubscribeEvent
	public void onNicknameChanged(GuildMemberUpdateNicknameEvent event) {
		String newNickOG = event.getNewNickname();
		User user = event.getUser();
		if (!user.isBot() && newNickOG != null) { // User should not be a Bot
			String newNick = newNickOG.toLowerCase(Locale.ROOT);
			// User should not be an Alt account
			if (newNick.contains("[z") && !newNick.contains("[alt")) {
				String newOffset = newNick.substring(newNick.indexOf("[z"));
				String oldNick = event.getOldNickname();
				Logger logger = LoggerFactory.getLogger("NicknameChanged");
				if (oldNick != null) {
					oldNick = oldNick.toLowerCase(Locale.ROOT);
					String oldOffset = oldNick.substring(oldNick.indexOf("[z"));
					// don't change the timezone if the offset is the same
					if (!newOffset.equals(oldOffset)) {
						if (Timezones.updateSpecificTimezone(user.getIdLong(), newOffset))
							logger.info("Updated Timezone of User: " + newNickOG);
						else
							logger.error("Could not save Timezone of User: " + newNickOG);
					}
				} else { // old nick could not be loaded but new nick is still there so load timezone anyway
					if (Timezones.updateSpecificTimezone(user.getIdLong(), newOffset))
						logger.info("Updated Timezone of User: " + event.getNewNickname());
					else
						logger.error("Could not save Timezone of User: " + event.getNewNickname());
				}
			}
		}
	}
	
	/**
	 * Is triggered when a Message is written
	 *
	 * @param event Message Receive Event
	 */
	@SubscribeEvent
	public void onReceiveMessage(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		Logger logger = LoggerFactory.getLogger("ReceivedMessage");
		String content = event.getMessage().getContentRaw().toLowerCase(Locale.ROOT);
		MessageChannel channel = event.getChannel();
		
		// These boolean are always false if written in a personal message
		boolean isAdmin = false;
		boolean isEventOrganizer = false;
		boolean isInstructor = false;
		
		Member authorMember = null;
		Guild guild;
		if (!event.isFromGuild()) {
			guild = event.getJDA().getGuildById(BotMain.ROLES.get("Guild"));
			if (guild != null) {
				authorMember = guild.getMember(event.getAuthor());
			}
		} else {
			guild = event.getGuild();
			authorMember = guild.getMember(event.getAuthor());
		}
		
		// Check the roles of the User
		if (authorMember != null) {
			List<Role> rolesOfUser = authorMember.getRoles();
			
			Long adminRole = BotMain.ROLES.get("Admin");
			Long eventOrganizerRole = BotMain.ROLES.get("Event_Organizer");
			Long instructorRole = BotMain.ROLES.get("Instructor");
			
			isAdmin = rolesOfUser.contains(adminRole == null ? null
					: guild.getRoleById(adminRole));
			isEventOrganizer = rolesOfUser.contains(eventOrganizerRole == null ? null
					: guild.getRoleById(eventOrganizerRole));
			isInstructor = rolesOfUser.contains(instructorRole == null ? null
					: guild.getRoleById(instructorRole));
		}
		
		boolean isOwner = event.getAuthor().getIdLong() == BotMain.ROLES.get("Owner");
		isAdmin = isAdmin || isOwner; // Owner is also admin
		
		if (content.length() != 0 && content.charAt(0) == '!') {
			logger.info("Received Message from " + event.getAuthor().getName() + " in channel "
					+ channel.getName() + ": " + event.getMessage().getContentRaw());
			String command;
			content = content.substring(1);
			if (content.indexOf(' ') != -1)
				command = content.substring(0, content.indexOf(' '));
			else
				command = content;
			switch (command) {
				case "help" -> Help.showHelp(isInstructor, isEventOrganizer, isAdmin,
						channel, content); // show Help Page
				case "ping" -> Ping.makePing(channel); // make a ping test
				case "time" -> Timezones
						.getTimezoneOfUserCommand(event, content); // get the Timezone of a User
				case "timezones" -> Timezones
						.getTimezoneOfAllUsersCommand(event); // print the Timezone of all Users
				case "trained" -> Trained
						.makeUserTrained(isInstructor, event); // give a User the Trained Role
				case "countdown" -> Countdowns.countdownCommand(isEventOrganizer, isOwner,
						event); // create a live countdown
				case "event" -> Event
						.eventCommand(isEventOrganizer, isOwner, event, content); // event command
				case "restart" -> BotStatus
						.restartBot(isAdmin, channel); // restarts the Bot connection
				case "reload" -> Reload.reloadMain(isAdmin, event,
						content); // reload the Config files or Timezones
				case "purge" -> Purge.purgeMessages(isOwner, event,
						content); // purges X Messages from the channel
				case "stop" -> BotStatus
						.stopBot(isOwner, channel); // stops the Bot, this takes a while
			}
		}
	}
	
}
