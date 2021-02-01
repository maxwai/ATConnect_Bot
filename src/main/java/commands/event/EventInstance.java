package commands.event;

import bot.BotEvents;
import emoji.Emoji;
import java.awt.Color;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import telegram.TelegramLogger;

/**
 * Internal Class that saves one Instance of an Event
 */
public class EventInstance {
	
	/**
	 * The default String when a parameter is not set
	 */
	public static final String notSet = "Not Set";
	/**
	 * Date Format for the input and output of the event Date
	 */
	public static final DateTimeFormatter sdfDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	/**
	 * Date Format for the input and output of the event Time
	 */
	public static final DateTimeFormatter sdfTime = DateTimeFormatter.ofPattern("HH:mm");
	/**
	 * Date Format for how to output the whole Date
	 */
	public static final DateTimeFormatter sdfComplete = DateTimeFormatter
			.ofPattern("dd.MM.yyyy HH:mm");
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Event Instance");
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
	 * List of all locations in this Event
	 */
	public final ArrayList<Location> locations = new ArrayList<>();
	/**
	 * Saved Guild Instance where the Event Embed is posted
	 */
	public final Guild guild;
	/**
	 * All Users that have the maybe Position
	 */
	public final ArrayList<Long> maybeUsers = new ArrayList<>();
	/**
	 * All Users that have the backup Position
	 */
	public final ArrayList<Long> backupUsers = new ArrayList<>();
	/**
	 * Toggle for the maybe position
	 */
	public boolean maybeToggle = true;
	/**
	 * Toggle for the backup position
	 */
	public boolean backupToggle = true;
	/**
	 * Toggle if voting is open or not
	 */
	public boolean vote = false;
	/**
	 * The Message Instance in the Guild with the Event Embed
	 */
	public Message eventEmbedMessage;
	/**
	 * The Message Instance in the Private Chat where the Help page is shown
	 */
	public Message commandsMessage;
	/**
	 * The Message Instance in the Private Chat with the Event Embed
	 */
	public Message eventPrivateEmbedMessage;
	/**
	 * The Channel in the Guild where the Event Embed is supposed to be
	 */
	public MessageChannel eventEmbedMessageChannel;
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
	 * This is the constructor for when an Event is retrieved by the XML Parser
	 *
	 * @param guild The Guild
	 * @param title The Title
	 * @param desc The Description
	 * @param startTime The Start Time
	 * @param stopTime The Stop Time
	 * @param eventDateSet If the event Date was set
	 * @param startTimeSet If the start Time was set
	 * @param stopTimeSet If the stop Time was set
	 * @param maybeToggle If maybe position is on/off
	 * @param backupToggle If backup position is on/off
	 * @param vote If voting is on/off
	 * @param commandsMessage The Message in the Private Chat with the Commands
	 * @param eventPrivateEmbedMessage The Embed Message in the Private Chat
	 * @param eventEmbedMessage The Embed Message with the Event
	 * @param eventEmbedMessageChannel The Channel where Event Embed is
	 * @param maybeUsers The Users in the maybe position
	 * @param backupUsers The Users in the backup position
	 * @param locations The Location Instances
	 */
	public EventInstance(@Nonnull Guild guild, @Nonnull String title, @Nonnull String desc,
			@Nullable TemporalAccessor startTime, @Nullable TemporalAccessor stopTime,
			boolean eventDateSet, boolean startTimeSet, boolean stopTimeSet, boolean maybeToggle,
			boolean backupToggle, boolean vote, @Nonnull Message commandsMessage,
			@Nonnull Message eventPrivateEmbedMessage, @Nonnull Message eventEmbedMessage,
			@Nonnull MessageChannel eventEmbedMessageChannel, @Nonnull ArrayList<Long> maybeUsers,
			@Nonnull ArrayList<Long> backupUsers, @Nonnull ArrayList<Location> locations) {
		this.guild = guild;
		this.title = title;
		this.description = desc;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.setEventDate = eventDateSet;
		this.setStartTime = startTimeSet;
		this.setEndTime = stopTimeSet;
		this.maybeToggle = maybeToggle;
		this.backupToggle = backupToggle;
		this.vote = vote;
		this.commandsMessage = commandsMessage;
		this.eventPrivateEmbedMessage = eventPrivateEmbedMessage;
		this.eventEmbedMessage = eventEmbedMessage;
		this.eventEmbedMessageChannel = eventEmbedMessageChannel;
		this.maybeUsers.addAll(maybeUsers);
		this.backupUsers.addAll(backupUsers);
		this.locations.addAll(locations);
		this.locations.forEach(location -> location.updateGuildAndParent(guild, this));
	}
	
	/**
	 * Creates an Event:
	 * - Sends the raw Event Embed in the Guild Channel
	 * - Sends the Help Embed as a Private Message
	 * - Sends a copy of the Event Embed in the Private Message
	 *
	 * @param event The MessageReceived Event
	 */
	public EventInstance(@Nonnull MessageReceivedEvent event) {
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
	public void deleteEvent() {
		eventEmbedMessage.delete()
				.queue(unused -> {}, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
	}
	
	/**
	 * Processes an Event Command
	 *
	 * @param event The MessageReceived Event
	 * @param command The Command that is after "!event "
	 */
	public void update(@Nonnull MessageReceivedEvent event, @Nonnull String command) {
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
									eventEmbedMessage.delete().queue(unused -> {},
											new ErrorHandler()
													.ignore(ErrorResponse.UNKNOWN_MESSAGE));
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
	void updateEmbeds(boolean deleteReactions) {
		MessageEmbed embed = getEventEmbed();
		eventEmbedMessage.editMessage(embed).queue(ignored -> addReactions(deleteReactions),
				new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> // in case deleted
						eventEmbedMessageChannel.sendMessage(embed)
								.queue(message -> eventEmbedMessage = message)));
		eventPrivateEmbedMessage.editMessage(embed).queue(unused -> {},
				new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> // in case deleted
								eventPrivateEmbedMessage.getChannel().sendMessage(embed)
										.queue(message -> eventPrivateEmbedMessage = message)));
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
	public void assignLocation(@Nonnull String emoji, @Nonnull User user) {
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
	void removeUser(@Nonnull Long user) {
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
	public String getTitle() {
		return title == null ? notSet : title;
	}
	
	/**
	 * Return the description
	 *
	 * @return The description or {@link #notSet} if null
	 */
	@Nonnull
	public String getDescription() {
		return description == null ? notSet : description;
	}
	
	/**
	 * Returns the Event Date in a String representation
	 *
	 * @return The String representation of the Event Date or {@link #notSet} if null
	 */
	@Nonnull
	public String getEventDate() {
		return setEventDate ? sdfDate.format(startTime) : notSet;
	}
	
	/**
	 * Returns the Start Time in a String representation
	 *
	 * @return The String representation of the Start Time or {@link #notSet} if null
	 */
	@Nonnull
	public String getStartTime() {
		return setStartTime ? sdfTime.format(startTime) : notSet;
	}
	
	/**
	 * Returns the Stop Time in a String representation
	 *
	 * @return The String representation of the Stop Time or {@link #notSet} if null
	 */
	@Nonnull
	public String getStopTime() {
		return setEndTime ? sdfTime.format(stopTime) : notSet;
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
		eventInfo += getEventDate() + "\n";
		eventInfo += Emoji.CLOCK_2 + " "; // add clock Emoji
		eventInfo += getStartTime() + " - ";
		eventInfo += getStopTime() + " [Z+0]";
		
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
}
