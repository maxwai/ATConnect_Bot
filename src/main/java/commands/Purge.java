package commands;

import bot.BotEvents;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Purge {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final Logger logger = LoggerFactory.getLogger("Purge Command");
	
	/**
	 * boolean to know if a purge is currently being done
	 */
	private static boolean isWorking = false;
	/**
	 * Lock for the {@link #isWorking} changes
	 */
	private static final Object lock = new Object();
	
	/**
	 * Will purge the given amount of Messages
	 *
	 * @param isOwner If the User is the Owner of the Bot
	 * @param event Event to get more information
	 * @param content The Content of the Message that was send.
	 */
	public static void purgeMessages(boolean isOwner, MessageReceivedEvent event, String content) {
		if (!event.isFromGuild()) {
			event.getChannel().sendMessage("Can't purge if not a Text Channel in a Guild.").queue();
			return;
		}
		TextChannel channel = event.getTextChannel();
		if (isOwner) { // only the Owner is allowed to purge Messages
			if (content.length() == 5) {
				channel.sendMessage("You have to specify how many messages to delete").queue();
				return;
			}
			// add 1 since the command should not count
			int size = 1 + Integer.parseInt(content.substring(6));
			logger.info("Purging " + size + " Messages");
			
			synchronized (lock) {
				if (isWorking) {
					channel.sendMessage("I'm busy right now...")
							.queue(message -> BotEvents.deleteMessageAfterXTime(message, 10));
					return;
				}
				isWorking = true;
			}
			
			int[] amountArray = new int[]{size};
			
			new Thread(() -> {
				boolean isWorkingTmp;
				synchronized (lock) {
					isWorkingTmp = isWorking;
				}
				while (isWorkingTmp) {
					List<Message> messages = channel.getHistory()
							.retrievePast(Math.min(amountArray[0], 100)).complete();
					
					amountArray[0] -= Math.min(amountArray[0], 100);
					
					if (messages.isEmpty())
						break;
					
					channel.purgeMessages(messages).forEach(CompletableFuture::join);
					
					if (amountArray[0] == 0)
						break;
					
					synchronized (lock) {
						isWorkingTmp = isWorking;
					}
				}
				
				synchronized (lock) {
					isWorking = false;
				}
				channel.sendMessage((size - 1) + " Messages Purged")
						.queue(BotEvents::addTrashcan);
				logger.info("Finished purging Messages");
			}).start();
		} else // User isn't the Owner
			channel.sendMessage("You don't have permission for this command").queue();
	}
}
