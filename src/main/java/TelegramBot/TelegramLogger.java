package TelegramBot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class TelegramLogger {
	
	private final Logger logger;
	private final String name;
	
	private TelegramLogger(String name) {
		this.name = name;
		logger = LoggerFactory.getLogger(name);
	}
	
	public static TelegramLogger getLogger(String name) {
		return new TelegramLogger(name);
	}
	
	public String getName() {
		return name;
	}
	
	public void trace(String message) {
		logger.trace(message);
		TelegramBot.sendLog(name, "Trace", message);
	}
	
	public void debug(String message) {
		logger.debug(message);
		TelegramBot.sendLog(name, "Debug", message);
	}
	
	public void info(String message) {
		logger.info(message);
		TelegramBot.sendLog(name, "Info", message);
	}
	
	public void warn(String message) {
		logger.warn(message);
		TelegramBot.sendImportantLog(name, "Warning", message);
	}
	
	public void error(String message) {
		logger.error(message);
		TelegramBot.sendImportantLog(name, "Error", message);
	}
	
}
