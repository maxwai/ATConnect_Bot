package Commands;

import Bot.BotEvents;
import Bot.BotMain;
import TelegramBot.TelegramLogger;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Reload {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Reload Command");
	
	/**
	 * Will reload the given Information (can be Config or Timezones)
	 *
	 * @param isAdmin the User is an Admin
	 * @param event Event to get more information
	 * @param channel The Channel where the Message was send.
	 * 		This may be removed in a further release since this can also be fetched with the event
	 * 		Instance
	 * @param content The Content of the Message that was send.
	 * 		This may be removed in a further release since this can also be fetched with the event
	 * 		Instance
	 */
	public static void reloadMain(boolean isAdmin, MessageReceivedEvent event,
			MessageChannel channel, String content) {
		if (isAdmin) { // only Admin is allowed to Reload the Config or Timezones
			if (content.equals("reload")) { // The User did not specify what to reload
				reloadNotSpecified(channel);
			} else {
				content = content.substring(7);
				switch (content) {
					case "config" -> reloadConfig(channel); // Reload Config files
					case "timezones", "timezone" -> reloadTimezones(event,
							channel); // Reload Timezones
					default -> reloadNotSpecified(channel);
				}
			}
		} else // User isn't an Admin
			channel.sendMessage("You don't have permission for this command").queue();
	}
	
	/**
	 * Reloads the Config
	 *
	 * @param channel The Channel where the Message was send.
	 *
	 * @see BotMain#reloadConfig
	 */
	private static void reloadConfig(MessageChannel channel) {
		logger.info("Reloading the Config");
		channel.sendMessage("reloading Config").queue(message -> {
			BotMain.reloadConfig(); // reload all Config files
			message.editMessage("Config reloaded successfully").queue();
		});
	}
	
	/**
	 * Reloads the Timezones of all Users
	 *
	 * @param event Event to get more information
	 * @param channel The Channel where the Message was send.
	 * 		This may be removed in a further release since this can also be fetched with the event
	 * 		Instance
	 */
	private static void reloadTimezones(MessageReceivedEvent event, MessageChannel channel) {
		logger.info("Reloading the Timezones");
		channel.sendMessage("reloading all user Timezones").queue(message -> {
			Timezones.updateTimezones(event.getJDA()); // reload all Timezones
			message.editMessage("Timezones reloaded").queue();
		});
	}
	
	/**
	 * The User tried to reload something without specifying or something not known
	 *
	 * @param channel The Channel where the Message was send.
	 */
	private static void reloadNotSpecified(MessageChannel channel) {
		channel.sendMessage("Please specify what to reload. Possible is: config, timezones")
				.queue(message -> BotEvents.deleteMessageAfterXTime(message, 10));
	}
}
