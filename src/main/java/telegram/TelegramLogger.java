package telegram;

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
	
	public static String convertNewLine(String message) {
		return message.replace("\n", " \\n ");
	}
	
	public void trace(String message) {
		message = convertNewLine(message);
		logger.trace(message);
		TelegramBots.sendLog(name, "Trace", message);
	}
	
	public void debug(String message) {
		message = convertNewLine(message);
		logger.debug(message);
		TelegramBots.sendLog(name, "Debug", message);
	}
	
	public void info(String message) {
		message = convertNewLine(message);
		logger.info(message);
		TelegramBots.sendLog(name, "Info", message);
	}
	
	public void warn(String message) {
		message = convertNewLine(message);
		logger.warn(message);
		TelegramBots.sendImportantLog(name, "Warning", message);
	}
	
	public void error(String message) {
		message = convertNewLine(message);
		logger.error(message);
		TelegramBots.sendImportantLog(name, "Error", message);
	}
	
}
