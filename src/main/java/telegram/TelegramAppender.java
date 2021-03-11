package telegram;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class TelegramAppender extends ConsoleAppender<LoggingEvent> {
	
	@Override
	protected void append(LoggingEvent eventObject) {
		super.append(eventObject);
		String loggerName = eventObject.getLoggerName();
		loggerName = loggerName
				.substring(loggerName.indexOf('.') == -1 ? 0 : loggerName.lastIndexOf('.') + 1);
		
		switch (eventObject.getLevel().toInt()) {
			case Level.INFO_INT, Level.TRACE_INT, Level.DEBUG_INT -> TelegramBots
					.sendLog(loggerName, eventObject.getLevel().toString(),
							eventObject.getFormattedMessage());
			case Level.ERROR_INT, Level.WARN_INT -> TelegramBots
					.sendImportantLog(loggerName, eventObject.getLevel().toString(),
							eventObject.getFormattedMessage());
		}
	}
}
