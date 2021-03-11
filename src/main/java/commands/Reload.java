package commands;

import bot.BotEvents;
import bot.BotMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Reload {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final Logger logger = LoggerFactory.getLogger("Reload Command");
	
	/**
	 * Will reload the given Information (can be Config or Timezones)
	 *
	 * @param isAdmin the User is an Admin
	 * @param event Event to get more information
	 * @param content The Content of the Message that was send.
	 */
	public static void reloadMain(boolean isAdmin, MessageReceivedEvent event, String content) {
		if (isAdmin) { // only Admin is allowed to Reload the Config or Timezones
			if (content.equals("reload")) { // The User did not specify what to reload
				reloadNotSpecified(event.getChannel());
			} else {
				content = content.substring(7);
				switch (content) {
					case "config" -> reloadConfig(event.getChannel()); // Reload Config files
					case "timezones", "timezone" -> reloadTimezones(event); // Reload Timezones
					default -> reloadNotSpecified(event.getChannel());
				}
			}
		} else // User isn't an Admin
			event.getChannel().sendMessage("You don't have permission for this command").queue();
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
	 */
	private static void reloadTimezones(MessageReceivedEvent event) {
		logger.info("Reloading the Timezones");
		event.getChannel().sendMessage("reloading all user Timezones...").queue(message -> {
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
