package Bot;

import Commands.BotStatus;
import Commands.Countdowns;
import Commands.Help;
import Commands.Ping;
import Commands.Purge;
import Commands.Reload;
import Commands.Timezones;
import Commands.Trained;
import TelegramBot.TelegramBots;
import TelegramBot.TelegramLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class BotEvents {
	
	private static final TelegramLogger logger = TelegramLogger.getLogger("BotStatus");
	
	/**
	 * Adds a Trashcan so that the Message can be easily deleted by users
	 *
	 * @param message The message where the Trashcan should be added
	 */
	public static void addTrashcan(Message message) {
		message.addReaction("\uD83D\uDDD1")
				.queue(); // add a reaction to make it easy to delete the post
	}
	
	/**
	 * Deletes the message after the given amount of time in Seconds
	 *
	 * @param message The message to delete
	 * @param seconds Time until the message should be deleted
	 */
	public static void deleteMessageAfterXTime(Message message, long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException ignored) {
		}
		message.delete().queue(); // delete the Message after X sec
	}
	
	/**
	 * Returns the Name of the User on the Server
	 *
	 * @param member User where we want the name
	 *
	 * @return Return the Nickname, or username if nickname is not available
	 */
	public static String getServerName(Member member) {
		String nickname = member.getNickname();
		if (nickname == null)
			nickname = member.getEffectiveName();
		return nickname;
	}
	
	@SubscribeEvent
	public void onDisconnect(DisconnectEvent event) {
		logger.error("Discord Bot Disconnected");
	}
	
	@SubscribeEvent
	public void onException(ExceptionEvent event) {
		if (event.isLogged()) {
			TelegramBots.sendImportantLog(logger.getName(), "Error", event.getCause().getMessage());
		} else {
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
		logger.warn("Discord Bot connection Resumed");
	}
	
	@SubscribeEvent
	public void onReconnected(ReconnectedEvent event) {
		logger.warn("Discord Bot reconnected successfully");
	}
	
	@SubscribeEvent
	public void onShutdown(ShutdownEvent event) {
		logger.warn("Discord Bot is shutting down");
	}
	
	/**
	 * Is triggered when an Emote is added / count is changed
	 *
	 * @param event Reaction add Event
	 */
	@SubscribeEvent
	public void onEmoteAdded(GuildMessageReactionAddEvent event) {
		if (event.getUser().isBot()) return; // don't react if the bot is adding this emote
		Member authorMember = event.getGuild().getMember(event.getUser());
		List<Role> rolesOfUser = authorMember != null ? authorMember.getRoles() : new ArrayList<>();
		boolean isAdmin =
				rolesOfUser.contains(event.getGuild().getRoleById(BotMain.ROLES.get("Admin"))) ||
						event.getUser().getIdLong() == BotMain.ROLES.get("Owner");
		event.retrieveMessage()
				.queue(message -> { // only do something if he is admin or the wastebasket was already here from the bot
					if (!message.getAuthor().isBot()) return; // delete only bot messages
					if ((isAdmin || message.getReactions().get(0).isSelf())
							&& event.getReactionEmote().isEmoji()) {
						String emoji = event.getReactionEmote().getEmoji();
						if (emoji.substring(0, emoji.length() - 1).equals("\uD83D\uDDD1")
								|| emoji.equals("\uD83D\uDDD1")) { // :wastebasket:
							TelegramLogger.getLogger("ReactionAdded")
									.info("deleting message because of :wastebasket: reaction");
							// check if this message was part of a Countdown
							if (Countdowns.messageIds.contains(message.getId()))
								// close that countdown since the message will be deleted
								Countdowns.closeSpecificThread(message.getId());
							message.delete().queue(); // delete the message
						}
					}
				});
	}
	
	/**
	 * Is triggered when the Nickname of a User is changed
	 *
	 * @param event Nickname Update Event
	 */
	@SubscribeEvent
	public void onNicknameChanged(GuildMemberUpdateNicknameEvent event) {
		String newNickOG = event.getNewNickname();
		User user = event.getUser();
		if (!user.isBot() && newNickOG != null) { // User should not be a Bot
			String newNick = newNickOG.toLowerCase(Locale.ROOT);
			// User should not be an Alt account
			if (newNick.contains("[z") && !newNick.contains("[alt")) {
				String newOffset = newNick.substring(newNick.indexOf("[z"));
				String oldNick = event.getOldNickname();
				TelegramLogger logger = TelegramLogger.getLogger("NicknameChanged");
				if (oldNick != null) {
					oldNick = oldNick.toLowerCase(Locale.ROOT);
					String oldOffset = oldNick.substring(oldNick.indexOf("[z"));
					// don't change the timezone if the offset is the same
					if (!newOffset.equals(oldOffset)) {
						if (Timezones.updateSpecificTimezone(user.getIdLong(), newOffset))
							logger.info("Updated Timezone of User: " + newNickOG);
						else
							logger.error("Could not save Timezone of User: " + newNickOG);
					}
				} else { // old nick could not be loaded but new nick is still there so load timezone anyway
					if (Timezones.updateSpecificTimezone(user.getIdLong(), newOffset))
						logger.info("Updated Timezone of User: " + event.getNewNickname());
					else
						logger.error("Could not save Timezone of User: " + event.getNewNickname());
				}
			}
		}
	}
	
	/**
	 * Is triggered when a Message is written
	 *
	 * @param event Message Receive Event
	 */
	@SubscribeEvent
	public void onReceiveMessage(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		TelegramLogger logger = TelegramLogger.getLogger("ReceivedMessage");
		String content = event.getMessage().getContentRaw().toLowerCase(Locale.ROOT);
		MessageChannel channel = event.getChannel();
		
		// These boolean are always false if written in a personal message
		boolean isAdmin = false;
		boolean isEventOrganizer = false;
		boolean isInstructor = false;
		
		// if this message is from a Guild/Server then check the roles of the User
		if (event.isFromGuild()) {
			Member authorMember = event.getGuild().getMember(event.getAuthor());
			List<Role> rolesOfUser =
					authorMember != null ? authorMember.getRoles() : new ArrayList<>();
			
			Long adminRole = BotMain.ROLES.get("Admin");
			Long eventOrganizerRole = BotMain.ROLES.get("Event_Organizer");
			Long instructorRole = BotMain.ROLES.get("Instructor");
			
			isAdmin = rolesOfUser.contains(adminRole == null ? null
					: event.getGuild().getRoleById(adminRole));
			isEventOrganizer = rolesOfUser.contains(eventOrganizerRole == null ? null
					: event.getGuild().getRoleById(eventOrganizerRole));
			isInstructor = rolesOfUser.contains(instructorRole == null ? null
					: event.getGuild().getRoleById(instructorRole));
		}
		boolean isOwner = event.getAuthor().getIdLong() == BotMain.ROLES.get("Owner");
		isAdmin = isAdmin || isOwner; // Owner is also admin
		
		if (content.length() != 0 && content.charAt(0) == '!') {
			logger.info("Received Message from " + event.getAuthor().getName() + " in channel "
					+ channel.getName() + ": " + content);
			String command;
			content = content.substring(1);
			if (content.indexOf(' ') != -1)
				command = content.substring(0, content.indexOf(' '));
			else
				command = content;
			switch (command) {
				case "help" -> Help.showHelp(isInstructor, isEventOrganizer, isAdmin,
						channel); // show Help Page
				case "ping" -> Ping.makePing(channel); // make a ping test
				case "time" -> Timezones
						.getTimezoneOfUserCommand(event, content); // get the Timezone of a User
				case "timezones" -> Timezones.getTimezoneOfAllUsersCommand(event.getGuild(),
						channel); // print the Timezone of all Users
				case "trained" -> Trained.makeUserTrained(isInstructor, event,
						channel); // give a User the Trained Role
				case "countdown" -> Countdowns.countdownCommand(isEventOrganizer, isOwner, event,
						channel); // create a live countdown
				case "restart" -> BotStatus
						.restartBot(isAdmin, channel); // restarts the Bot connection
				case "reload" -> Reload.reloadMain(isAdmin, event, channel,
						content); // reload the Config files or Timezones
				case "purge" -> Purge.purgeMessages(isOwner, channel,
						content); // purges X Messages from the channel
				case "stop" -> BotStatus
						.stopBot(isOwner, channel); // stops the Bot, this takes a while
			}
		}
	}
	
}
