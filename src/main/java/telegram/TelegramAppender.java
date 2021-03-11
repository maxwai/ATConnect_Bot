package telegram;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class TelegramAppender extends ConsoleAppender<LoggingEvent> {
	
	@Override
	protected void append(LoggingEvent eventObject) {
		super.append(eventObject);
		
		switch (eventObject.getLevel().toInt()) {
			case Level.INFO_INT, Level.TRACE_INT, Level.DEBUG_INT -> TelegramBots
					.sendLog(eventObject.getLoggerName(), eventObject.getLevel().toString(),
							eventObject.getMessage());
			case Level.ERROR_INT, Level.WARN_INT -> TelegramBots
					.sendImportantLog(eventObject.getLoggerName(),
							eventObject.getLevel().toString(), eventObject.getMessage());
		}
	}
}
