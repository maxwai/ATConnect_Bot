package commands;

import bot.BotEvents;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import telegram.TelegramLogger;

public class Event {
	
	/**
	 * A Map of all Events in the system, mapped to the author ID
	 */
	private static final Map<Long, EventInstance> eventsMap = new HashMap<>();
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Event Command");
	
	/**
	 * Will get to the correct Event Command
	 *
	 * @param isEventOrganizer If this User is an Event Organizer
	 * @param isOwner If the User is the Owner of the Bot
	 * @param event Event to get more information
	 * @param content The Content of the Message that was send.
	 */
	public static void eventCommand(boolean isEventOrganizer, boolean isOwner,
			MessageReceivedEvent event, String content) {
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
				if (command.equals("create")) { // create a new event
					if (event.isFromGuild())
						createEvent(event);
					else
						event.getChannel().sendMessage(
								"You need to start the event from the channel where the event Text should be displayed")
								.queue();
				} else if (command.equals("delete")) { //delete the event
					if (eventsMap.containsKey(event.getAuthor().getIdLong())) {
						eventsMap.get(event.getAuthor().getIdLong()).deleteEvent();
						eventsMap.remove(event.getAuthor().getIdLong());
					} else
						event.getChannel().sendMessage("You don't have an event setup").queue();
				} else {
					if (eventsMap.containsKey(event.getAuthor().getIdLong())) {
						eventsMap.get(event.getAuthor().getIdLong())
								.update(event, command);
					} else {
						event.getChannel().sendMessage("You have to create an event first").queue();
					}
					if (!event.getChannelType().equals(ChannelType.PRIVATE)) {
						BotEvents.deleteMessageAfterXTime(event.getMessage(), 5);
					}
				}
			}
		} else // isn't Event Organizer or Owner
			event.getChannel().sendMessage("You don't have permission for this command").queue();
	}
	
	private static void createEvent(MessageReceivedEvent event) {
		event.getMessage().delete().queue();
		if (eventsMap.containsKey(event.getAuthor().getIdLong()))
			event.getChannel().sendMessage("You can only have one active event at a time").queue();
		else
			eventsMap.put(event.getAuthor().getIdLong(), new EventInstance(event));
	}
	
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
		
		private static final MessageEmbed embed2 = new EmbedBuilder()
				.setTitle("Event Creation (2/2)")
				.setDescription("Here is the List of commands you should do next")
				.addField("`!event location`", """
						Adds an Airport and it's positions
						`!event location <Airport Code>: <Pos1>, <Pos2>, ...`
						`!event location delete` Will delete all Locations""", false)
				.addField("`!event toggle XY`", """
						Toggle the special Positions
						Special Positions:
						`Maybe`, `Backup`
						`!event toggle maybe`""", false)
				.addField("`!event vote`", "Will toggle on/off the voting for the positions", false)
				.addField("`!event previous`", "Shows the previous page of Help", false)
				.build();
		
		private static final String notSet = "Not Set";
		
		private final ArrayList<ArrayList<String>> locations = new ArrayList<>();
		
		private String title;
		private String description;
		
		private TemporalAccessor startTime;
		private TemporalAccessor stopTime;
		private boolean setEventDate = false;
		private boolean setStartTime = false;
		private boolean setEndTime = false;
		
		private boolean maybeToggle = true;
		private boolean backupToggle = true;
		private boolean vote = false;
		
		private Message commandsMessage;
		private Message eventEmbedMessage;
		private Message eventPrivateEmbedMessage;
		private MessageChannel eventEmbedMessageChannel;
		
		
		private EventInstance(MessageReceivedEvent event) {
			logger.info("Starting to create an event");
			MessageEmbed embed = getEventEmbed();
			event.getAuthor().openPrivateChannel()
					.queue(channel -> {
						channel.sendMessage("We will continue the creation of the Event here:")
								.queue();
						channel.sendMessage(embed1).queue(message -> commandsMessage = message);
						channel.sendMessage(embed)
								.queue(message -> eventPrivateEmbedMessage = message);
					});
			event.getChannel().sendMessage(embed).queue(message -> eventEmbedMessage = message);
			eventEmbedMessageChannel = event.getChannel();
		}
		
		private static TemporalAccessor setTime(MessageReceivedEvent event, String argument,
				TemporalAccessor time) {
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
		
		private void deleteEvent() {
			eventEmbedMessage.delete().queue();
		}
		
		private void update(MessageReceivedEvent event, String command) {
			String content = event.getMessage().getContentRaw().substring(7);
			content = content.contains(" ") ? content.substring(content.indexOf(" ") + 1) : null;
			switch (command) {
				case "title" -> title = content;
				case "desc", "description" -> description = content;
				case "date" -> {
					if (content != null) {
						try {
							String timeStart;
							if (startTime != null) {
								timeStart = sdfTime.format(startTime);
							} else {
								timeStart = "00:00";
							}
							String timeStop;
							if (stopTime != null) {
								timeStop = sdfTime.format(stopTime);
							} else {
								timeStop = "00:00";
							}
							
							startTime = sdfComplete.parse(content + " " + timeStart);
							stopTime = sdfComplete.parse(content + " " + timeStop);
							setEventDate = true;
						} catch (DateTimeException | NumberFormatException e) {
							event.getChannel().sendMessage("Date is not in the wanted Format")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						}
					}
				}
				case "start" -> {
					startTime = setTime(event, content, startTime);
					setStartTime = true;
				}
				case "end", "stop" -> {
					stopTime = setTime(event, content, stopTime);
					setEndTime = true;
				}
				case "location" -> {
					if (content != null) {
						if (content.equals("delete")) { // delete all locations
							locations.clear();
						} else if (locations.size() == 10) { // location amount is reached
							event.getChannel().sendMessage("You can't have more than 10 locations")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						} else { // add a new location
							try {
								ArrayList<String> positions = new ArrayList<>();
								positions.add(content.substring(0, content.indexOf(':')));
								content = content.substring(content.indexOf(':') + 1);
								if (content.charAt(0) == ' ')
									content = content.substring(1);
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
								locations.add(positions);
							} catch (IndexOutOfBoundsException e) {
								event.getChannel()
										.sendMessage("The command was not how it was expected")
										.queue(message -> BotEvents
												.deleteMessageAfterXTime(message, 5));
							}
						}
					}
				}
				case "toggle" -> {
					if (content != null) {
						switch (content.toLowerCase(Locale.ROOT)) {
							case "maybe" -> maybeToggle = !maybeToggle;
							case "backup" -> backupToggle = !backupToggle;
							default -> event.getChannel().sendMessage("wrong toggle description")
									.queue(message -> BotEvents
											.deleteMessageAfterXTime(message, 5));
						}
					}
				}
				case "next" -> commandsMessage.editMessage(embed2).queue();
				case "previous" -> commandsMessage.editMessage(embed1).queue();
				case "vote" -> vote = !vote;
				case "move" -> {
					if (event.isFromGuild()) {
						List<TextChannel> mentionedChannels = event.getMessage()
								.getMentionedChannels();
						if (mentionedChannels.size() == 1) {
							eventEmbedMessage.delete().queue();
							eventEmbedMessageChannel = mentionedChannels.get(0);
							Object lock = new Object();
							eventEmbedMessageChannel.sendMessage(getEventEmbed())
									.queue(message -> {
										eventEmbedMessage = message;
										synchronized (lock) {
											lock.notify();
										}
									});
							try {
								synchronized (lock) {
									lock.wait(5000);
								}
							} catch (InterruptedException ignored) {
							}
						} else
							event.getChannel()
									.sendMessage("You have to mention exactly one Channel").queue();
					} else
						event.getChannel().sendMessage("You have to send this command in a Guild")
								.queue();
				}
				default -> event.getChannel().sendMessage("You tried an unknown command")
						.queue(message -> BotEvents.deleteMessageAfterXTime(message, 5));
			}
			MessageEmbed embed = getEventEmbed();
			eventEmbedMessage.editMessage(embed).queue(ignored -> addReactions(event));
			eventPrivateEmbedMessage.editMessage(embed).queue();
		}
		
		private void addReactions(MessageReceivedEvent event) {
			// we need to retrieve the message since Reactions are immutable
			eventEmbedMessageChannel.retrieveMessageById(eventEmbedMessage.getIdLong())
					.queue(message -> {
						List<MessageReaction> reactions = message.getReactions();
						// remove every reaction
						reactions.forEach(messageReaction -> message.removeReaction(
									messageReaction.getReactionEmote().getAsReactionCode())
									.queue(unused -> {}, Throwable::printStackTrace));
						
						// add the reactions if toggled
						if (vote && locations.size() != 0) {
							int i = 0;
							if (locations.size() == 10) {
								message.addReaction(Emoji.ZERO).queue();
								i = 1;
							}
							for (; i < locations.size(); i++)
								message.addReaction(Emoji.numbersList.get(i)).queue();
						}
					});
		}
		
		private MessageEmbed getEventEmbed() {
			EmbedBuilder eb = new EmbedBuilder();
			
			eb.setTitle(title == null ? notSet : title);
			
			eb.setColor(Color.CYAN);
			
			if (setStartTime) {
				eb.setTimestamp(startTime);
			}
			
			String eventInfo = Emoji.CALENDAR_SPIRAL + " "; // add calender Emoji
			eventInfo += (setEventDate ? sdfDate.format(startTime) : notSet) + "\n";
			eventInfo += Emoji.CLOCK_2 + " "; // add clock Emoji
			eventInfo += (setStartTime ? sdfTime.format(startTime) : notSet) + " - ";
			eventInfo += (setEndTime ? sdfTime.format(stopTime) : notSet) + " [Z+0]";
			
			eb.addField("Event Info:", eventInfo, false);
			
			eb.addField("Description", description == null ? notSet : description, false);
			
			if (locations.size() != 0) {
				int i = 0;
				if (locations.size() == 10) {
					eb.addField(Emoji.ZERO + " " + locations.get(0).get(0) + " (0)", "-", true);
					i = 1;
				}
				for (; i < locations.size(); i++) {
					String emoji = Emoji.numbersList.get(i);
					eb.addField(emoji + " " + locations.get(i).get(0) + " (0)", "-", true);
				}
			}
			
			eb.addBlankField(false);
			
			if (maybeToggle)
				eb.addField("Maybe (0)", "-", true);
			if (backupToggle)
				eb.addField("Backup (0)", "-", true);
			
			return eb.build();
		}
	}
}
