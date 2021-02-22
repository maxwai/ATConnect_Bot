package commands;

import bot.BotEvents;
import bot.BotMain;
import commands.event.EventInstance;
import commands.event.Location;
import emoji.Emoji;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import telegram.TelegramLogger;
import xml.XMLParser;

public class Event {
	
	/**
	 * A Map of all Events in the system, mapped to the author ID. And with the current active
	 * Event.
	 * <p>
	 * The Array List has following layout: {@code [index_Active, EventInstance, EventInstance,
	 * ...]}
	 */
	private static final HashMap<Long, List<Object>> eventsMap = new HashMap<>();
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Event Command");
	
	/**
	 * Hashmap with all active Messages to choose a Event.
	 * <p>
	 * The Object array has following setup: {@code [Message, userID]}
	 */
	private static final HashMap<Long, Object[]> messages = new HashMap<>();
	
	/**
	 * Will get to the correct Event Command
	 *
	 * @param isEventOrganizer If this User is an Event Organizer
	 * @param isOwner If the User is the Owner of the Bot
	 * @param event Event to get more information
	 * @param content The Content of the Message that was send.
	 */
	public static void eventCommand(boolean isEventOrganizer, boolean isOwner,
			@Nonnull MessageReceivedEvent event, String content) {
		if (isEventOrganizer || isOwner) { // only event organizer or Owner are allowed
			if (content.length() == 5 || content.substring(6).equals("help")) { // print help page
				Help.sendEventHelpPage(event.getChannel());
			} else {
				content = content.substring(6); // remove the "event " portion
				String command;
				if (content.indexOf(' ') != -1)
					command = content.substring(0, content.indexOf(' '));
				else
					command = content;
				switch (content) {
					case "create" -> { // create a new event
						if (event.isFromGuild())
							createEvent(event);
						else
							event.getChannel().sendMessage(
									"You need to start the event from the channel where the event Text should be displayed")
									.queue();
					}
					case "delete" -> {  //delete the event
						if (eventsMap.containsKey(event.getAuthor().getIdLong())) {
							List<Object> list = eventsMap.get(event.getAuthor().getIdLong());
							int index = (int) list.get(0);
							((EventInstance) list.get(index)).deleteEvent();
							list.remove(index);
							if (list.size() == 1) {
								eventsMap.remove(event.getAuthor().getIdLong());
							} else if (index == list.size())
								list.set(0, list.size() - 1);
							logger.info("Deleting Event");
						} else
							event.getChannel().sendMessage("You don't have an event setup").queue();
					}
					case "switch" -> { // switch between events
						if (eventsMap.containsKey(event.getAuthor().getIdLong())) {
							printSwitchEmbed(event, eventsMap.get(event.getAuthor().getIdLong()));
						} else
							event.getChannel().sendMessage("You don't have any event setup")
									.queue();
					}
					default -> { // event command, is handled by event Instance
						if (eventsMap.containsKey(event.getAuthor().getIdLong())) {
							List<Object> list = eventsMap.get(event.getAuthor().getIdLong());
							int index = (int) list.get(0);
							((EventInstance) list.get(index)).update(event, command);
						} else {
							event.getChannel().sendMessage("You have to create an event first")
									.queue();
						}
						if (!event.getChannelType().equals(ChannelType.PRIVATE)) {
							BotEvents.deleteMessageAfterXTime(event.getMessage(), 5);
						}
					}
				}
			}
		} else // isn't Event Organizer or Owner
			event.getChannel().sendMessage("You don't have permission for this command").queue();
	}
	
	/**
	 * Saves all Events to the Config.xml
	 */
	public static void saveEvents() {
		XMLParser.saveEvents(eventsMap);
	}
	
	public static void loadEvents(@Nonnull JDA jda) {
		eventsMap.putAll(XMLParser.getEvents(jda));
	}
	
	/**
	 * Will print the Embed to choose a
	 *
	 * @param event The MessageReceived Event
	 * @param list The List of Events
	 */
	private static void printSwitchEmbed(@Nonnull MessageReceivedEvent event,
			@Nonnull List<Object> list) {
		if (list.size() == 2) {
			event.getChannel().sendMessage("You only have one event setup").queue();
		} else if (list.size() == 3) {
			String eventString = ((EventInstance) list.get(((int) list.get(0)) % 2 + 1)).getTitle();
			event.getChannel()
					.sendMessage("Switched to the only other Event you have: " + eventString)
					.queue();
		} else {
			EmbedBuilder eb = new EmbedBuilder();
			
			eb.setColor(Color.RED);
			
			eb.setTitle("Choose your Event:");
			
			for (int i = 1; i < list.size(); i++) {
				eb.addField(
						Emoji.numbersList.get(i - 1) + " " + ((EventInstance) list.get(i))
								.getTitle(),
						((EventInstance) list.get(i)).getDescription(), false);
			}
			
			eb.setFooter("This message will be deleted in 60sec");
			
			event.getChannel().sendMessage(eb.build()).queue(message -> {
				for (int i = 1; i < list.size(); i++) {
					message.addReaction(Emoji.numbersList.get(i - 1)).queue();
				}
				
				messages.put(message.getIdLong(), new Object[]{message, event.getAuthor()});
				
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
	}
	
	/**
	 * Will check if the user can create a new event, and if yes, will create a new event
	 * It is expected that it is already checked if the user has the permission to create events
	 *
	 * @param event The MessageReceivedEvent where the command was send
	 */
	private static void createEvent(@Nonnull MessageReceivedEvent event) {
		event.getMessage().delete().queue();
		if (!eventsMap.containsKey(event.getAuthor().getIdLong())) {
			ArrayList<Object> list = new ArrayList<>();
			list.add(1);
			list.add(new EventInstance(event));
			eventsMap.put(event.getAuthor().getIdLong(), list);
		} else {
			List<Object> list = eventsMap.get(event.getAuthor().getIdLong());
			list.add(new EventInstance(event));
			list.set(0, list.size() - 1);
		}
		
	}
	
	/**
	 * Will check if the Reaction was added on a Event related message
	 *
	 * @param message The Message were the reaction was added
	 * @param reactionEmote The Reaction that was added
	 * @param user The User that Reacted
	 * @param channel The Channel where the Message was send.
	 *
	 * @return {@code true} if the reaction was Event related. In this case nothing should happen
	 * 		next. {@code false} if the reaction should be further investigated
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean reactionAdded(@Nonnull Message message,
			@Nonnull ReactionEmote reactionEmote, @Nonnull User user,
			@Nonnull MessageChannel channel) {
		
		if (messages.containsKey(message.getIdLong())) {
			// reactions to Switch Event Embed are only Emojis
			if (reactionEmote.isEmoji()) {
				String emoji = Emoji.getCleanedUpEmoji(reactionEmote.getEmoji());
				List<Object> list = eventsMap.get(user.getIdLong());
				// check if reaction is for event switching
				if (Emoji.numbersList.contains(emoji)
						&& Emoji.numbersList.indexOf(emoji) < list.size() - 1) {
					list.set(0, Emoji.numbersList.indexOf(emoji) + 1);
					message.delete().queue();
					messages.remove(message.getIdLong());
				}
			}
			// remove only the reaction of the user, not all reactions
			message.removeReaction(reactionEmote.getAsReactionCode(), user)
					.queue(unused -> {}, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
			return true;
		}
		
		Set<Long> keys = eventsMap.keySet();
		for (Long key : keys) { // check every User ID to find a matching one
			List<Object> list = eventsMap.get(key);
			for (int i = 1; i < list.size(); i++) {
				EventInstance eventInstance = (EventInstance) list.get(i);
				if (eventInstance.eventEmbedMessage.getIdLong() == message.getIdLong()) {
					if (reactionEmote
							.isEmoji()) { // reactions to the main Event Embed are only Emojis
						String emoji = Emoji.getCleanedUpEmoji(reactionEmote.getEmoji());
						// check if reaction is for position voting
						if (emoji.equals(Emoji.COUCH) ||
								emoji.equals(Emoji.GREY_QUESTION) ||
								emoji.equals(Emoji.X) ||
								Emoji.numbersList.contains(emoji)) {
							eventInstance.assignLocation(channel, emoji, user);
						} else if (emoji.equals(Emoji.WASTEBASKET)) { // We want to delete the Event
							boolean isOwner = user.getIdLong() == BotMain.ROLES.get("Owner");
							// Only event owner and bot Owner should be able to delete an event
							if (user.getIdLong() == key || isOwner) {
								eventInstance.deleteEvent();
								list.remove(i);
								
								if (list.size() == 1) {
									eventsMap.remove(key);
								} else if ((int) list.get(0) == list.size())
									list.set(0, list.size() - 1);
								
								logger.info("Deleting Event");
								return true; // don't go further since message is deleted
							} else {
								channel.sendMessage("Only the Event creator can delete the event")
										.queue(message1 -> BotEvents
												.deleteMessageAfterXTime(message1, 10));
							}
						}
					}
					// remove only the reaction of the user, not all reactions
					message.removeReaction(reactionEmote.getAsReactionCode(), user).queue();
					return true;
				}
				// Go through every location and check if a position choosing message is
				// where the reaction was added
				for (Location location : eventInstance.locations) {
					if (location.messages.containsKey(message.getIdLong())) {
						return location.addUser(reactionEmote, user, message);
					}
				}
			}
		}
		return false;
	}
}
