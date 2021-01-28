package telegram;

import bot.BotMain;
import bot.Config;
import java.util.ArrayList;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramBots {
	
	private static final TelegramLogger logger = TelegramLogger.getLogger("Telegram");
	private static DiscordBotTelegramBot botAll;
	private static DiscordBotTelegramBot botImportant;
	private static BotSession botSessionAll;
	private static BotSession botSessionImportant;
	private static String chatID;
	
	public static void setupBots() {
		ArrayList<String[]> infos = Config.getTelegramInfo();
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
	}
	
	public static void closeBots() {
		logger.info("Stopping all Telegram Bots");
		if(botSessionAll != null)
			botSessionAll.stop();
		if(botSessionImportant != null)
			botSessionImportant.stop();
	}
	
	public static void sendImportantLog(String clazz, String level, String message) {
		message = level + "\n" + clazz + "\n\n" + message;
		
		if (chatID != null && botSessionImportant != null && botSessionAll != null) {
			SendMessage sendMessage = new SendMessage(chatID, message);
			sendMessage(botImportant, sendMessage);
			
			sendMessage = new SendMessage(chatID, message);
			sendMessage(botAll, sendMessage);
		}
	}
	
	public static void sendLog(String clazz, String level, String message) {
		message = level + "\n" + clazz + "\n\n" + message;
		
		if (chatID != null && botSessionImportant != null && botSessionAll != null) {
			SendMessage sendMessage = new SendMessage(chatID, message);
			sendMessage(botAll, sendMessage);
		}
	}
	
	public static void sendMessage(TelegramLongPollingBot bot, SendMessage message) {
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
