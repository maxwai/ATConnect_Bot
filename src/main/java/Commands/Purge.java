package Commands;

import Bot.BotEvents;
import TelegramBot.TelegramLogger;
import net.dv8tion.jda.api.entities.MessageChannel;

public class Purge {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Purge Command");
	
	/**
	 * Will purge the given amount of Messages
	 *
	 * @param isOwner If the User is the Owner of the Bot
	 * @param channel The Channel where the Message was send.
	 * @param content The Content of the Message that was send.
	 */
	public static void purgeMessages(boolean isOwner, MessageChannel channel, String content) {
		if (isOwner) { // only the Owner is allowed to purge Messages
			// add 1 since the command should not count
			int size = 1 + Integer.parseInt(content.substring(6));
			logger.info("Purging " + size + " Messages");
			channel.getIterableHistory() // Get the whole History
					.takeAsync(size) // Take a list of the last X Messages
					.thenAccept(channel::purgeMessages) // purge all Messages in that list
					.thenAccept(unused -> channel.sendMessage((size - 1) + " Messages Purged")
							.queue(BotEvents::addTrashcan));
		} else // User isn't the Owner
			channel.sendMessage("You don't have permission for this command").queue();
	}
}
