package telegram;

import bot.BotMain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import xml.XMLParser;

public class TelegramBots {
	
	private static final Logger logger = LoggerFactory.getLogger("Telegram");
	private static final String TELEGRAM_API_ERROR_TEXT = "api.telegram.org:443 failed to respond";
	private static final String TELEGRAM_GATEWAY_ERROR_TEXT =
			"{\"ok\":false,\"error_code\":502,\"description\":\"Bad Gateway\"}";
	private static DiscordBotTelegramBot botAll;
	private static DiscordBotTelegramBot botImportant;
	private static BotSession botSessionAll;
	private static BotSession botSessionImportant;
	private static String chatID;
	private static List<List<Object>> queue = new ArrayList<>();
	
	public static void setupBots() {
		ArrayList<String[]> infos = XMLParser.getTelegramBots();
		if (infos == null) {
			queue = null;
			return; // no Telegram Bot available
		}
		String usernameAll = infos.get(0)[0];
		String tokenAll = infos.get(0)[1];
		String usernameImportant = infos.get(1)[0];
		String tokenImportant = infos.get(1)[1];
		chatID = infos.get(2)[0];
		if (usernameAll == null || tokenAll == null || usernameImportant == null
				|| tokenImportant == null || chatID == null)
			throw new IllegalArgumentException("The Telegram Bot was not complete");
		botAll = new DiscordBotTelegramBot(tokenAll, usernameAll);
		botImportant = new DiscordBotTelegramBot(tokenImportant, usernameImportant);
		
		try {
			TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
			botSessionAll = botApi.registerBot(botAll);
			botSessionImportant = botApi.registerBot(botImportant);
			logger.info("Started all Telegram Bots");
		} catch (TelegramApiException e) {
			logger.error("The info in the Telegram Bot was not correct");
			e.printStackTrace();
		}
		queue.forEach(objects -> {
			String message = (String) objects.get(1);
			if ((int) objects.get(0) == 1) {
				SendMessage sendMessage = new SendMessage(chatID, message);
				sendMessage(botImportant, sendMessage);
			}
			SendMessage sendMessage = new SendMessage(chatID, message);
			sendMessage(botAll, sendMessage);
		});
		queue.clear();
		queue = null;
	}
	
	public static void closeBots() {
		logger.info("Stopping all Telegram Bots");
		if (botSessionAll != null)
			botSessionAll.stop();
		if (botSessionImportant != null)
			botSessionImportant.stop();
	}
	
	public static void sendImportantLog(String clazz, String level, String message) {
		if (message.equals(TELEGRAM_API_ERROR_TEXT) || message.equals(TELEGRAM_GATEWAY_ERROR_TEXT))
				return;
		message = level + "\n" + clazz + "\n\n" + message;
		
		if (chatID != null && botSessionImportant != null && botSessionAll != null) {
			SendMessage sendMessage = new SendMessage(chatID, message);
			sendMessage(botImportant, sendMessage);
			
			sendMessage = new SendMessage(chatID, message);
			sendMessage(botAll, sendMessage);
		} else if (queue != null) {
			queue.add(Arrays.asList(1, message));
		}
	}
	
	static void sendLog(String clazz, String level, String message) {
		if (message.equals(TELEGRAM_API_ERROR_TEXT) || message.equals(TELEGRAM_GATEWAY_ERROR_TEXT))
			return;
		message = level + "\n" + clazz + "\n\n" + message;
		
		if (chatID != null && botSessionImportant != null && botSessionAll != null) {
			SendMessage sendMessage = new SendMessage(chatID, message);
			sendMessage(botAll, sendMessage);
		} else if (queue != null) {
			queue.add(Arrays.asList(0, message));
		}
	}
	
	private static void sendMessage(TelegramLongPollingBot bot, SendMessage message) {
		try {
			bot.execute(message);
		} catch (TelegramApiException e) {
			logger.error("something went wrong while sending a Telegram Message");
			e.printStackTrace();
		}
	}
	
	
	private static class DiscordBotTelegramBot extends TelegramLongPollingBot {
		
		private final String BOT_TOKEN;
		private final String BOT_USERNAME;
		
		private DiscordBotTelegramBot(String token, String username) {
			BOT_TOKEN = token;
			BOT_USERNAME = username;
		}
		
		@Override
		public String getBotUsername() {
			return BOT_USERNAME;
		}
		
		@Override
		public String getBotToken() {
			return BOT_TOKEN;
		}
		
		@Override
		public void onUpdateReceived(Update update) {
			if (update.hasMessage() && update.getMessage().hasText()) {
				String message_text = update.getMessage().getText();
				if (message_text.equals("stop")) {
					logger.warn("Stopping Bot");
					BotMain.disconnectBot(); // stop the Bot
					TelegramBots.closeBots();
				}
			}
		}
	}
}
