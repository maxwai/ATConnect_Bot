package Commands;

import Bot.BotEvents;
import Bot.Config;
import java.time.Instant;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Countdowns {
	
	/**
	 * Stack of all the Ids of the Countdown messages
	 */
	public static final Stack<String> messageIds = new Stack<>();
	/**
	 * Stack of all the Countdown Threads
	 */
	private static final Stack<CountdownsThread> countdowns = new Stack<>();
	/**
	 * The Logger for Log Messages
	 */
	private static final Logger logger = LoggerFactory.getLogger("Countdown");
	
	/**
	 * Will add a new countdown to the List
	 *
	 * @param isEventOrganizer If this User is an Event Organizer
	 * @param isOwner If the User is the Owner of the Bot
	 * @param event Event to get more information
	 * @param channel The Channel where the Message was send.
	 * 		This may be removed in a further release since this can also be fetched with the event
	 * 		Instance
	 */
	public static void countdownCommand(boolean isEventOrganizer, boolean isOwner,
			MessageReceivedEvent event, MessageChannel channel) {
		if (isEventOrganizer || isOwner) // Countdown can only be done by Event Organizer or Owner
			startNewCountdown(event, channel);
		else // isn't Event Organizer or Owner
			channel.sendMessage("You don't have permission for this command").queue();
	}
	
	/**
	 * Start a new Countdown with the given Information in the Event
	 *
	 * @param event The Event with the Countdown Information
	 */
	private static void startNewCountdown(MessageReceivedEvent event, MessageChannel channel) {
		logger.debug("detected countdown command");
		String content = event.getMessage().getContentRaw();
		try {
			channel.deleteMessageById(event.getMessageId()).queue();
			content = content.substring(11);
			
			// take the time information of the countdown
			int day = Integer.parseInt(content.substring(0, content.indexOf('.')));
			content = content.substring(content.indexOf('.') + 1);
			int month = Integer.parseInt(content.substring(0, content.indexOf('.')));
			content = content.substring(content.indexOf('.') + 1);
			int year = Integer.parseInt(content.substring(0, content.indexOf(' ')));
			content = content.substring(content.indexOf(' ') + 1);
			int hour = Integer.parseInt(content.substring(0, content.indexOf(':')));
			content = content.substring(content.indexOf(':') + 1);
			int minutes;
			
			// take the extra text that needs to be displayed after the countdown
			if (content.length() <= 2) {
				minutes = Integer.parseInt(content);
				content = "";
			} else {
				minutes = Integer.parseInt(content.substring(0, content.indexOf(' ')));
				content = content.substring(content.indexOf(' '));
			}
			
			// Parse the date to a Instant Instance
			Instant date = Instant.parse(year + "-" + String.format("%02d", month) + "-" +
					String.format("%02d", day) + "T" + String.format("%02d", hour) + ":" +
					String.format("%02d", minutes) + ":00Z");
			
			// Check if the time is not in th past
			if (date.getEpochSecond() < Instant.now().getEpochSecond()) {
				channel.sendMessage(
						"You tried making a countdown in the past. (Message will delete after 10 sec)")
						.queue(message -> BotEvents.deleteMessageAfterXTime(message, 10));
				logger.warn("User tried making a countdown in the past");
				return;
			}
			logger.info("Starting countdown thread");
			// start the Thread that changes the Message to the current Countdown
			CountdownsThread countdownsThread = new CountdownsThread(channel, content, date);
			countdownsThread.start();
			countdowns.push(countdownsThread);
			saveAllCountdowns(); // save the countdown in the event of unexpected failure
		} catch (NumberFormatException | StringIndexOutOfBoundsException ignored) { // The command had some parsing error
			channel.sendMessage("Something went wrong with your command try again\n" +
					"Format is: `!countdown DD.MM.YYYY HH:mm <additional Text>`").queue();
		}
	}
	
	/**
	 * Restarts the Countdowns that are in the Countdowns.cfg file
	 *
	 * @param jda The JDA to get the Channels and Messages
	 */
	public static void restartCountdowns(JDA jda) {
		Config.getCountdowns().forEach(countdownInfos -> {
			// Pick up the channel where the Message is
			TextChannel channel = jda.getTextChannelById(countdownInfos[0]);
			if (channel
					!= null) { // check if the Channel is still there, if not ignore this Countdown
				channel.retrieveMessageById(countdownInfos[1])
						.queue(message -> { // message still exists
							Instant date = Instant.parse(countdownInfos[3]);
							if (date.getEpochSecond() < Instant.now()
									.getEpochSecond()) { // check if the Countdown is in the past
								message.editMessage("Countdown finished").queue();
								BotEvents.addTrashcan(
										message); // add a reaction to make it easy to delete the post
							} else {
								logger.info("Added Countdown Thread from existing Countdown");
								CountdownsThread countdownsThread =
										new CountdownsThread(channel, countdownInfos[1],
												countdownInfos[2], date);
								countdownsThread.start();
								countdowns.push(countdownsThread);
							}
						}, throwable -> logger
								.warn("Removing one Countdown where Message is deleted")); // If the message is deleted
			} else {
				logger.warn("Removing one Countdown where Channel is deleted");
			}
		});
	}
	
	/**
	 * Save all active Countdowns in the Countdowns.cfg
	 */
	private static void saveAllCountdowns() {
		Config.saveCountdowns(countdowns);
	}
	
	/**
	 * Close a specific Countdown with its Message ID
	 *
	 * @param messageId The Message ID of the Countdown that needs to be closed
	 */
	public static void closeSpecificThread(String messageId) {
		int index = messageIds.indexOf(messageId);
		if (index == -1) return;
		CountdownsThread thread = countdowns.get(index);
		thread.interrupt();
		countdowns.remove(index);
		messageIds.remove(index);
	}
	
	/**
	 * Close all Thread and save them in Countdowns.cfg
	 */
	public static void closeAllThreads() {
		saveAllCountdowns();
		countdowns.forEach(Thread::interrupt);
	}
	
	/**
	 * Thread Class for the Countdowns
	 */
	public static class CountdownsThread extends Thread {
		
		/**
		 * Lock for {@link #stop}
		 */
		private final Object lock = new Object();
		/**
		 * The Channel where the Message of the Countdown is
		 */
		private final MessageChannel channel;
		/**
		 * The Text that is added at the end of the Countdown
		 */
		private final String text;
		/**
		 * The Date of the End of the Countdown
		 */
		private final Instant date;
		/**
		 * boolean to know if the Thread should be closed
		 */
		private boolean stop = false;
		/**
		 * The Message ID of the Countdown
		 */
		private String messageId;
		
		/**
		 * Constructor when the Countdown is restored after a restart
		 *
		 * @param channel The Channel where the Message of the Countdown is
		 * @param messageId The Message ID of the Countdown
		 * @param text The Text that is added at the end of the Countdown
		 * @param date The Date of the End of the Countdown
		 */
		private CountdownsThread(MessageChannel channel, String messageId, String text,
				Instant date) {
			this.text = text;
			this.date = date;
			this.channel = channel;
			this.messageId = messageId;
			messageIds.push(this.messageId);
		}
		
		/**
		 * Constructor for a new Countdown
		 *
		 * @param channel The Channel where the Message of the Countdown will be
		 * @param text The Text that is added at the end of the Countdown
		 * @param date The Date of the End of the Countdown
		 */
		private CountdownsThread(MessageChannel channel, String text, Instant date) {
			this.text = text;
			this.date = date;
			this.channel = channel;
			
			logger.info("sending message");
			channel.sendMessage(computeLeftTime()[0] + text).queue(message -> {
				synchronized (lock) {
					this.messageId = message.getId();
					messageIds.push(this.messageId);
					lock.notify(); // notify the Thread that the Message Id is available
				}
			});
		}
		
		/**
		 * Compute the String Array with the information for the Countdowns.cfg File
		 *
		 * @return A String Array with following layout: {@code {channelId, messageId, text, date}}
		 */
		public String[] getInfos() {
			return new String[]{channel.getId(), messageId, text, date.toString()};
		}
		
		/**
		 * The main Function of this Thread
		 */
		@Override
		public void run() {
			synchronized (lock) {
				if (messageId == null) { // If don't yet have a message ID wait for it
					try {
						lock.wait();
					} catch (InterruptedException ignored) {
					}
				}
			}
			while (!stop) { // don't stop until wanted
				Object[] info = computeLeftTime();
				if (info[0] instanceof Boolean) { // if this countdown is at it's end
					logger.info("Countdown finished removing it from the Threads List");
					channel.editMessageById(messageId, "Countdown finished")
							.queue(BotEvents::addTrashcan,
									// add a reaction to make it easy to delete the post
									throwable -> logger
											.warn("Removing one Countdown where Message is deleted")); // Message was deleted, don't do anything here
					countdowns.remove(this);
					return;
				}
				logger.info("editing message: " + info[0]);
				channel.editMessageById(messageId, info[0] + text)
						.queue(message -> {}, throwable -> {
							// Message was deleted, delete this Thread
							stop = true;
							countdowns.remove(this);
							logger.warn("Removing one Countdown where Message is deleted");
						});
				try {
					long sleepTime = (Long) info[1]; // sleep until the next change
					//noinspection BusyWait
					sleep(sleepTime < 5000 ? 60000 : sleepTime);
				} catch (InterruptedException ignored) {
				}
			}
		}
		
		@Override
		public void interrupt() {
			stop = true;
			super.interrupt();
		}
		
		/**
		 * Compute the time until the next change
		 *
		 * @return An Array with: {@code {String countdownMessage, Long timeUntilNextChange}}
		 */
		private Object[] computeLeftTime() {
			long differenceOG = date.getEpochSecond() - Instant.now().getEpochSecond();
			long dayDiff = TimeUnit.DAYS.convert(differenceOG, TimeUnit.SECONDS);
			long differenceHour = differenceOG - TimeUnit.SECONDS.convert(dayDiff, TimeUnit.DAYS);
			long hourDiff = TimeUnit.HOURS.convert(differenceHour, TimeUnit.SECONDS);
			long differenceMinutes =
					differenceHour - TimeUnit.SECONDS.convert(hourDiff, TimeUnit.HOURS);
			long minutesDiff = TimeUnit.MINUTES.convert(differenceMinutes, TimeUnit.SECONDS);
			long differenceSeconds =
					differenceMinutes - TimeUnit.SECONDS.convert(minutesDiff, TimeUnit.MINUTES);
			
			if (dayDiff > 7) {
				if (dayDiff % 7 == 0)
					return new Object[]{(dayDiff / 7) + " weeks left",
							TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
				return new Object[]{(dayDiff / 7) + " weeks and " + (dayDiff % 7) + " days left",
						TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
			}
			if (dayDiff > 3 || dayDiff > 0 && hourDiff == 0)
				return new Object[]{dayDiff + " days left",
						TimeUnit.MILLISECONDS.convert(differenceHour, TimeUnit.SECONDS)};
			if (dayDiff > 0)
				return new Object[]{dayDiff + " days and " + hourDiff + " left",
						TimeUnit.MILLISECONDS.convert(differenceMinutes, TimeUnit.SECONDS)};
			if (hourDiff > 6 || hourDiff > 0 && minutesDiff == 0)
				return new Object[]{hourDiff + " hours left",
						TimeUnit.MILLISECONDS.convert(differenceMinutes, TimeUnit.SECONDS)};
			if (hourDiff > 0)
				return new Object[]{hourDiff + " hours and " + minutesDiff + " minutes left",
						TimeUnit.MILLISECONDS.convert(differenceSeconds, TimeUnit.SECONDS)};
			if (minutesDiff > 0)
				return new Object[]{minutesDiff + " minutes left",
						TimeUnit.MILLISECONDS.convert(differenceSeconds, TimeUnit.SECONDS)};
			return new Object[]{true};
		}
	}
	
}
