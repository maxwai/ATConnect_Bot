package bot;

import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotStatusEvents {
	
	private static final Logger logger = LoggerFactory.getLogger("BotStatus");
	private static Thread shutdownThread;
	
	@SubscribeEvent
	public void onDisconnect(DisconnectEvent event) {
		shutdownThread = new Thread(() -> {
			try {
				Thread.sleep(60000);
				logger.error("Discord Bot Disconnected");
				shutdownThread = null;
			} catch (InterruptedException ignored) {
			}
		});
		shutdownThread.start();
	}
	
	@SubscribeEvent
	public void onException(ExceptionEvent event) {
		if (!event.isLogged()) {
			logger.error(event.getCause().getMessage());
			event.getCause().printStackTrace();
		}
	}
	
	@SubscribeEvent
	public void onReady(ReadyEvent event) {
		logger.warn("Discord Bot is Ready");
	}
	
	@SubscribeEvent
	public void onResumed(ResumedEvent event) {
		if (shutdownThread != null) {
			shutdownThread.interrupt();
		} else
			logger.warn("Discord Bot connection Resumed");
	}
	
	@SubscribeEvent
	public void onReconnected(ReconnectedEvent event) {
		if (shutdownThread != null) {
			shutdownThread.interrupt();
		} else
			logger.warn("Discord Bot reconnected successfully");
	}
	
	@SubscribeEvent
	public void onShutdown(ShutdownEvent event) {
		logger.warn("Discord Bot is shutting down");
	}
}
