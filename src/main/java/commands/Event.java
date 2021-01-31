package commands;

import bot.BotEvents;
import bot.BotMain;
import commands.Event.EventInstance.Location;
import emoji.Emoji;
import java.awt.Color;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import telegram.TelegramLogger;

public class Event {
	
	/**
	 * A Map of all Events in the system, mapped to the author ID. And with the current active
	 * Event.
	 * <p>
	 * The Array List has following layout: {@code [index_Active, EventInstance, EventInstance,
	 * ...]}
	 */
	private static final Map<Long, List<Object>> eventsMap = new HashMap<>();
	
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
					message.delete()
							.queue(unused -> {},
									new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
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
							eventInstance.assignLocation(emoji, user);
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
	
	/**
	 * Internal Class that saves one Instance of an Event
	 */
	public static class EventInstance {
		
		/**
		 * Date Format for the input and output of the event Date
		 */
		private static final DateTimeFormatter sdfDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		/**
		 * Date Format for the input and output of the event Time
		 */
		private static final DateTimeFormatter sdfTime = DateTimeFormatter.ofPattern("HH:mm");
		/**
		 * Date Format for how to output the whole Date
		 */
		private static final DateTimeFormatter sdfComplete = DateTimeFormatter
				.ofPattern("dd.MM.yyyy HH:mm");
		
		/**
		 * First Embed Help Page of creating the Event
		 */
		private static final MessageEmbed embed1 = new EmbedBuilder()
				.setTitle("Event Creation (1/2)")
				.setDescription("Here is the List of commands you should do next")
				.addField("`!event title`", "Set the Title of your Event\n`!event title <Title>`",
						false)
				.addField("`!event desc`", """
						Set the Description of your Event
						`!event desc <Description>`""", false)
				.addField("`!event date`", """
						Set the Date of your Event
						`!event date DD.MM.YYYY`""", false)
				.addField("`!event start`", """
						Set the Start Time of your Event
						`!event start HH:mm`""", false)
				.addField("`!event end`", """
						Set the End Time of your Event
						`!event end HH:mm`""", false)
				.addField("`!event next`", "Shows the next page of Help", false)
				.build();
		
		/**
		 * Second Embed Help Page of creating the Event
		 */
		private static final MessageEmbed embed2 = new EmbedBuilder()
				.setTitle("Event Creation (2/2)")
				.setDescription("Here is the List of commands you should do next")
				.addField("`!event location`", """
								Adds an Airport and it's positions
								`!event location <Airport Code>: <Pos1>, <Pos2>, ...`
								`!event location delete` Will delete all Locations
								`!event location delete <Airport Code>` Will delete a specific position""",
						false)
				.addField("`!event toggle XY`", """
						Toggle the special Positions
						Special Positions:
						`Maybe`, `Backup`
						`!event toggle maybe`""", false)
				.addField("`!event vote`", "Will toggle on/off the voting for the positions", false)
				.addField("`!event previous`", "Shows the previous page of Help", false)
				.addField("", "Get more help with `!help event`", false)
				.build();
		
		/**
		 * The default String when a parameter is not set
		 */
		private static final String notSet = "Not Set";
		
		/**
		 * List of all locations in this Event
		 */
		private final ArrayList<Location> locations = new ArrayList<>();
		/**
		 * Saved Guild Instance where the Event Embed is posted
		 */
		private final Guild guild;
		
		/**
		 * All Users that have the maybe Position
		 */
		private final ArrayList<Long> maybeUsers = new ArrayList<>();
		/**
		 * All Users that have the backup Position
		 */
		private final ArrayList<Long> backupUsers = new ArrayList<>();
		
		/**
		 * The Title of the Event
		 */
		private String title;
		/**
		 * The Description of the event
		 */
		private String description;
		
		/**
		 * The start Time of the Event
		 */
		private TemporalAccessor startTime;
		/**
		 * The end Time of the Event. This one only has the Time right and not the date.
		 */
		private TemporalAccessor stopTime;
		
		/**
		 * Boolean to know if the event Date is already Set
		 */
		private boolean setEventDate = false;
		/**
		 * Boolean to know if the start Time is already Set
		 */
		private boolean setStartTime = false;
		/**
		 * Boolean to know if the end Time is already Set
		 */
		private boolean setEndTime = false;
		
		/**
		 * Toggle for the maybe position
		 */
		private boolean maybeToggle = true;
		/**
		 * Toggle for the backup position
		 */
		private boolean backupToggle = true;
		
		/**
		 * Toggle if voting is open or not
		 */
		private boolean vote = false;
		
		/**
		 * The Message Instance in the Private Chat where the Help page is shown
		 */
		private Message commandsMessage;
		/**
		 * The Message Instance in the Private Chat with the Event Embed
		 */
		private Message eventPrivateEmbedMessage;
		/**
		 * The Message Instance in the Guild with the Event Embed
		 */
		private Message eventEmbedMessage;
		/**
		 * The Channel in the Guild where the Event Embed is supposed to be
		 */
		private MessageChannel eventEmbedMessageChannel;
		
		/**
		 * Creates an Event:
		 * - Sends the raw Event Embed in the Guild Channel
		 * - Sends the Help Embed as a Private Message
		 * - Sends a copy of the Event Embed in the Private Message
		 *
		 * @param event The MessageReceived Event
		 */
		private EventInstance(@Nonnull MessageReceivedEvent event) {
			logger.info("Starting to create an event");
			guild = event.getGuild();
			MessageEmbed embed = getEventEmbed();
			// start a private chat
			logger.info("Sending Private Message");
			event.getAuthor().openPrivateChannel()
					.queue(channel -> {
						channel.sendMessage("We will continue the creation of the Event here:")
								.queue();
						channel.sendMessage(embed1).queue(message -> commandsMessage = message);
						channel.sendMessage(embed)
								.queue(message -> eventPrivateEmbedMessage = message);
					});
			eventEmbedMessageChannel = event.getChannel();
			// send the Event Embed
			eventEmbedMessageChannel.sendMessage(embed)
					.queue(message -> eventEmbedMessage = message);
		}
		
		/**
		 * parses the time to a Temporal Accessor
		 *
		 * @param event The event to be able to respond
		 * @param argument The time in String representation
		 * @param time The Date already set, can be null
		 *
		 * @return The Temporal Accessor with the time
		 */
		@Nullable
		private static TemporalAccessor setTime(@Nonnull MessageReceivedEvent event,
				@Nullable String argument, @Nullable TemporalAccessor time) {
			if (argument != null) {
				try {
					String date =
							(time != null ? sdfDate.format(time) : sdfDate.format(Instant.now()))
									+ " ";
					
					time = sdfComplete.parse(date + argument);
				} catch (DateTimeException | NumberFormatException e) {
					event.getChannel().sendMessage("Time is not in the wanted Format")
							.queue(message -> BotEvents
									.deleteMessageAfterXTime(message, 5));
				}
			}
			return time;
		}
		
		/**
		 * Deletes this Event
		 */
		private void deleteEvent() {
			eventEmbedMessage.delete().queue();
		}
		
		/**
		 * Processes an Event Command
		 *
		 * @param event The MessageReceived Event
		 * @param command The Command that is after "!event "
		 */
		private void update(@Nonnull MessageReceivedEvent event, @Nonnull String command) {
			String content = event.getMessage().getContentRaw().substring(7);
			content = content.contains(" ") ? content.substring(content.indexOf(" ") + 1) : null;
			boolean deleteReactions = false;
			switch (command) {
				case "title" -> title = content; // set the title
				case "desc", "description" -> description = content; // set the description
				case "date" -> { // set the event date
					if (content != null) {
						try {
							String timeStart;
							// check if we have a start Time, if not default to 00:00
							if (startTime != null) {
								timeStart = sdfTime.format(startTime);
							} else {
								timeStart = "00:00";
							}
							String timeStop;
							// check if we have a end Time, if not default to 00:00
							if (stopTime != null) {
								timeStop = sdfTime.format(stopTime);
							} else {
								timeStop = "00:00";
							}
							
							startTime = sdfComplete.parse(content + " " + timeStart);
							stopTime = sdfComplete.parse(content + " " + timeStop);
							setEventDate = true;
						} catch (DateTimeException | NumberFormatException e) { // Date Parse error
							event.getChannel().sendMessage("Date is not in the wanted Format")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						}
					}
				}
				case "start" -> { // set the Start time
					startTime = setTime(event, content, startTime);
					setStartTime = true;
				}
				case "end", "stop" -> { // set the Stop time
					stopTime = setTime(event, content, stopTime);
					setEndTime = true;
				}
				case "location" -> { // location command
					if (content != null) {
						if (content.equals("delete")) { // delete all locations
							locations.clear();
							deleteReactions = true;
						} else if (content.contains("delete")) { // delete specific location
							int size = locations.size();
							for (Location location : locations) {
								if (location.location
										.equals(content.substring(content.indexOf(' ') + 1))) {
									locations.remove(location);
									break;
								}
							}
							if (size == locations.size()) // No location deleted
								event.getChannel().sendMessage("Unknown location")
										.queue(message -> BotEvents
												.deleteMessageAfterXTime(message, 5));
							deleteReactions = true;
						} else if (locations.size() == 10) { // location max amount is reached
							event.getChannel().sendMessage("You can't have more than 10 locations")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						} else { // add a new location
							try {
								ArrayList<String> positions = new ArrayList<>();
								String locationString = content.substring(0, content.indexOf(':'));
								content = content.substring(content.indexOf(':') + 1);
								if (content.charAt(0) == ' ') // delete space in front
									content = content.substring(1);
								// continue as long as there is String left
								while (!content.equals("")) {
									if (content.contains(",")) {
										positions.add(content.substring(0, content.indexOf(",")));
										content = content.substring(content.indexOf(",") + 1);
									} else {
										positions.add(content);
										content = "";
									}
									if (!content.equals("") && content.charAt(0) == ' ')
										content = content.substring(1);
								}
								if (positions.size() > 11) // Too many positions in this location
									event.getChannel().sendMessage(
											"Can't have more then 10 positions per location")
											.queue(message -> BotEvents
													.deleteMessageAfterXTime(message, 5));
								else {
									// remove location with the same name if present
									locations.removeIf(
											location -> location.location.equals(locationString));
									locations.add(new Location(locationString, positions,
											eventEmbedMessage.getGuild(), this));
									deleteReactions = true;
								}
							} catch (IndexOutOfBoundsException e) {
								event.getChannel()
										.sendMessage("The command was not how it was expected")
										.queue(message -> BotEvents
												.deleteMessageAfterXTime(message, 5));
							}
						}
					}
				}
				case "toggle" -> { // Toggle command
					if (content != null) {
						switch (content.toLowerCase(Locale.ROOT)) {
							case "maybe" -> { // toggle maybe position
								maybeToggle = !maybeToggle;
								maybeUsers.clear();
								deleteReactions = true;
							}
							case "backup" -> { // toggle backup position
								backupToggle = !backupToggle;
								backupUsers.clear();
								deleteReactions = true;
							}
							default -> event.getChannel().sendMessage("wrong toggle description")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						}
					}
				}
				case "next" -> commandsMessage.editMessage(embed2)
						.queue(); // show page 2 of Help Page
				case "previous" -> commandsMessage.editMessage(embed1)
						.queue(); // show page 1 of Help Page
				case "vote" -> { // toggle voting on/off
					vote = !vote;
					deleteReactions = true;
				}
				case "move" -> { // move the Embed to a different channel
					if (event.isFromGuild()) { // This message must come from a Server
						List<TextChannel> mentionedChannels = event.getMessage()
								.getMentionedChannels();
						if (mentionedChannels.size() == 1) {
							Object lock = new Object();
							mentionedChannels.get(0)
									.sendMessage(getEventEmbed()) // send new Embed
									.queue(message -> {
										// delete old Event Embed message
										eventEmbedMessage.delete().queue();
										// save new channel
										eventEmbedMessageChannel = mentionedChannels.get(0);
										eventEmbedMessage = message;
										synchronized (lock) {
											lock.notify();
										}
									}, new ErrorHandler()
											.handle(ErrorResponse.MISSING_PERMISSIONS, e ->
													eventEmbedMessageChannel.sendMessage(
															"Don't have permission to send Messages to that Channel")
															.queue()));
							try {
								synchronized (lock) {
									// wait for the message to be send
									lock.wait(5000);
								}
							} catch (InterruptedException ignored) {
							}
						} else // User mentioned multiple or no channels
							event.getChannel()
									.sendMessage("You have to mention exactly one Channel").queue();
					} else // Not from Guild
						event.getChannel().sendMessage("You have to send this command in a Guild")
								.queue();
				}
				default -> event.getChannel().sendMessage("You tried an unknown command")
						.queue(message -> BotEvents.deleteMessageAfterXTime(message, 5));
			}
			updateEmbeds(deleteReactions);
		}
		
		/**
		 * Update the Event Embed Message
		 *
		 * @param deleteReactions If the Reactions should be reposted because a change may have
		 * 		occurred
		 */
		private void updateEmbeds(boolean deleteReactions) {
			MessageEmbed embed = getEventEmbed();
			eventEmbedMessage.editMessage(embed).queue(ignored -> addReactions(deleteReactions),
					new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> // in case deleted
							eventEmbedMessageChannel.sendMessage(embed)
									.queue(message -> eventEmbedMessage = message)));
			eventPrivateEmbedMessage.editMessage(embed).queue(); // cannot be deleted
		}
		
		/**
		 * Add the needed Reactions to the Embed Message
		 *
		 * @param deleteReactions if the Reactions should be first removed
		 */
		private void addReactions(boolean deleteReactions) {
			// we need to retrieve the message since Reactions are immutable
			eventEmbedMessageChannel.retrieveMessageById(eventEmbedMessage.getIdLong())
					.queue(message -> {
						List<MessageReaction> reactions = message.getReactions();
						// remove every reaction
						if (deleteReactions) {
							reactions.forEach(messageReaction -> message.removeReaction(
									messageReaction.getReactionEmote().getAsReactionCode())
									.queue(unused -> {}, Throwable::printStackTrace));
						}
						
						// add the reactions if toggled
						if (vote) {
							if (locations.size() != 0) {
								for (int i = 0; i < locations.size(); i++)
									message.addReaction(Emoji.numbersList.get(i)).queue();
							}
							if (maybeToggle)
								message.addReaction(Emoji.GREY_QUESTION).queue();
							if (backupToggle)
								message.addReaction(Emoji.COUCH).queue();
							
							message.addReaction(Emoji.X).queue();
						}
					});
		}
		
		/**
		 * Will post the Position Embed for the chosen location, or remove the user from all
		 * positions
		 *
		 * @param emoji The Emoji of the Reaction
		 * @param user The User that reacted
		 */
		private void assignLocation(@Nonnull String emoji, @Nonnull User user) {
			Long userID = user.getIdLong();
			if (vote) { // only do something if voting is on
				switch (emoji) {
					case Emoji.X -> removeUser(userID); // remove User from position
					case Emoji.COUCH -> { // add user to backup
						removeUser(userID);
						backupUsers.add(userID);
					}
					case Emoji.GREY_QUESTION -> { // add user to maybe
						removeUser(userID);
						maybeUsers.add(userID);
					}
					default -> {
						// post Position chooser Embed for the Location
						if (Emoji.numbersList.contains(emoji)) {
							try {
								Location tmpLocation = locations
										.get(Emoji.numbersList.indexOf(emoji));
								user.openPrivateChannel().queue(privateChannel ->
										tmpLocation.postPositionEmbed(privateChannel, user));
							} catch (IndexOutOfBoundsException ignored) { // emoji was no for a location
							}
						}
					}
				}
				updateEmbeds(false); // redraw the Embeds
			}
		}
		
		/**
		 * Removes the User from all positions
		 *
		 * @param user The User that needs to be removed
		 */
		private void removeUser(@Nonnull Long user) {
			maybeUsers.remove(user);
			backupUsers.remove(user);
			for (Location location : locations) {
				location.deleteUser(user);
			}
		}
		
		/**
		 * Return the title
		 *
		 * @return The Title or {@link #notSet} if null
		 */
		@Nonnull
		private String getTitle() {
			return title == null ? notSet : title;
		}
		
		/**
		 * Return the description
		 *
		 * @return The description or {@link #notSet} if null
		 */
		@Nonnull
		private String getDescription() {
			return description == null ? notSet : description;
		}
		
		/**
		 * Will return the Embed for the Event
		 *
		 * @return The Event Embed
		 */
		@Nonnull
		private MessageEmbed getEventEmbed() {
			EmbedBuilder eb = new EmbedBuilder();
			
			eb.setColor(Color.CYAN);
			
			eb.setTitle(getTitle());
			
			if (setStartTime) {
				eb.setTimestamp(startTime);
			}
			
			String eventInfo = Emoji.CALENDAR_SPIRAL + " "; // add calender Emoji
			eventInfo += (setEventDate ? sdfDate.format(startTime) : notSet) + "\n";
			eventInfo += Emoji.CLOCK_2 + " "; // add clock Emoji
			eventInfo += (setStartTime ? sdfTime.format(startTime) : notSet) + " - ";
			eventInfo += (setEndTime ? sdfTime.format(stopTime) : notSet) + " [Z+0]";
			
			eb.addField("Event Info:", eventInfo, false);
			
			eb.addField("Description", getDescription(), false);
			
			if (locations.size() != 0) { // add all location with the Users of that location
				for (int i = 0; i < locations.size(); i++) {
					String emoji = Emoji.numbersList.get(i);
					eb.addField(emoji + " " + locations.get(i).getTitle(),
							locations.get(i).getContent(guild), true);
				}
			}
			
			eb.addBlankField(false);
			
			if (maybeToggle) // add maybe position if toggled
				eb.addField(Emoji.GREY_QUESTION + " Maybe (" + maybeUsers.size() + ")",
						getContent(maybeUsers, guild), true);
			if (backupToggle) // add backup position if toggled
				eb.addField(Emoji.COUCH + " Backup (" + backupUsers.size() + ")",
						getContent(backupUsers, guild), true);
			
			return eb.build();
		}
		
		/**
		 * Will return all Users in that position
		 *
		 * @param users The List of Users to display
		 * @param guild The Guild where the Event is
		 *
		 * @return The String that can be added to the Embed
		 */
		@Nonnull
		private String getContent(@Nonnull ArrayList<Long> users, @Nonnull Guild guild) {
			if (users.size() == 0)
				return "-";
			StringBuilder content = new StringBuilder();
			for (Long user : users) {
				content.append(BotEvents.getServerName(guild.getMemberById(user)))
						.append("\n");
			}
			return content.substring(0, content.length() - 1);
		}
		
		/**
		 * Internal class that represents one Location with it's Positions
		 */
		static class Location {
			
			/**
			 * The Location Name
			 */
			private final String location;
			/**
			 * The List of positions
			 */
			private final List<String> positions;
			/**
			 * The List of reactions in the same order as {@link #positions}.
			 * <p>
			 * The Reactions have two possible Classes: {@link String} or {@link Emote}
			 */
			private final ArrayList<Object> reactions;
			/**
			 * The Event Instance of that Location
			 */
			private final EventInstance parent;
			
			/**
			 * The Users at that Location
			 */
			private final ArrayList<Long> users = new ArrayList<>();
			/**
			 * The Positions of the Users at that Location. Same order as {@link #users}
			 */
			private final ArrayList<String> userPositions = new ArrayList<>();
			
			/**
			 * Hashmap with all active Messages to choose a position.
			 * <p>
			 * The Object array has following setup: {@code [Message, userID]}
			 */
			private final HashMap<Long, Object[]> messages = new HashMap<>();
			
			
			/**
			 * Will create a Location with it's positions
			 *
			 * @param location The Name of the Location
			 * @param positions List of all Position Names
			 * @param guild The Guild of the Event
			 * @param parent The Event Instance calling this Constructor
			 */
			private Location(@Nonnull String location, @Nonnull ArrayList<String> positions,
					@Nonnull Guild guild, @Nonnull EventInstance parent) {
				this.location = location;
				this.positions = positions;
				this.parent = parent;
				
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
			private String getTitle() {
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
			private String getContent(@Nonnull Guild guild) {
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
			private boolean addUser(@Nonnull ReactionEmote reactionEmote, @Nonnull User user,
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
			private void deleteUser(@Nonnull Long user) {
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
			private void postPositionEmbed(@Nonnull MessageChannel channel, @Nonnull User user) {
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
						message.delete()
								.queue(unused -> {},
										new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
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
	}
}
