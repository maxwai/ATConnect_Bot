package Commands;

import Bot.BotMain;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotStatus {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final Logger logger = LoggerFactory.getLogger("Bot Status");
	
	/**
	 * Will restart the bot Connection
	 *
	 * @param isAdmin If the User is an Admin
	 * @param channel The Channel where the Message was send.
	 */
	public static void restartBot(boolean isAdmin, MessageChannel channel) {
		if (isAdmin) { // only Admin is allowed to restart the Bot
			BotMain.restartIDs[0] = channel.getIdLong(); // save the channel ID for later
			logger.info("Restarting Bot");
			channel.sendMessage("restarting Bot").queue(message -> {
				synchronized (BotMain.lock) {
					BotMain.restartIDs[1] = message.getIdLong(); // save the message ID for later
					BotMain.lock
							.notify(); // notify another Thread that the Bot can be restarted now
				}
			});
			BotMain.restartBot(); // restart Bot
		} else // User isn't an Admin
			channel.sendMessage("You don't have permission for this command").queue();
	}
	
	/**
	 * Will Stop the Bot
	 *
	 * @param isOwner If the User is the Owner of the Bot
	 * @param channel The Channel where the Message was send.
	 */
	public static void stopBot(boolean isOwner, MessageChannel channel) {
		if (isOwner) { // only the Owner is allowed to stop the Bot
			logger.info("Stopping Bot");
			channel.sendMessage("stopping the Bot. Bye...").queue();
			BotMain.disconnectBot(); // stop the Bot
		} else // User isn't the Owner
			channel.sendMessage("You don't have permission for this command").queue();
	}
}
