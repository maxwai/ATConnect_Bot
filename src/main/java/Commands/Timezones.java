package Commands;

import Bot.BotEvents;
import Bot.BotMain;
import Bot.Config;
import TelegramBot.TelegramLogger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Timezones {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Timezones");
	/**
	 * Date Format for how to output the local Time of a User
	 */
	private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("EEE HH:mm")
			.withLocale(Locale.ENGLISH);
	/**
	 * Map with all the Timezones of the Users
	 */
	private static Map<Long, String> timezones = new HashMap<>();
	
	/**
	 * Will get the local times of the Users mentioned
	 *
	 * @param event Event to get more information
	 * @param content The Content of the Message that was send.
	 */
	public static void getTimezoneOfUserCommand(MessageReceivedEvent event, String content) {
		if (content.length() != 4) { // check if Users are mentioned
			content = content.substring(5)/*.toLowerCase(Locale.ROOT)*/; // is already LowerCase
			ArrayList<Long> members = new ArrayList<>();
			StringBuilder output = new StringBuilder("```\n"); // begin building the output message
			while (!content.equals("")) { // loop until no member mentioned is left
				if (content.charAt(0) == '<') { // member was mentioned with a tag
					members.add(Long.valueOf(content.substring(3, content.indexOf(">"))));
					content = content.substring(content.indexOf(">") + 1);
				} else { // Member was mentioned with (part of) his Guild name
					final String name;
					if (content.contains(",")) { // there are more names after this one
						name = content.substring(0, content.indexOf(','));
						content = content.substring(content.indexOf(',') + 1);
					} else { // there are no more names after this one
						name = content;
						content = ""; // empty content for exit
					}
					final int size = members.size(); // save how many members are in the list now
					event.getGuild().getMembers().forEach(member -> {
						if (size == members.size()) { // if no member was added continue
							String serverName = BotEvents.getServerName(member)
									.toLowerCase(Locale.ROOT);
							// ignore Bots and doesn't need to be a complete match
							if (!serverName.contains("[alt") && (serverName.contains(name)))
								members.add(member.getIdLong());
						}
					});
					if (size == members.size()) { // User was not found
						String error = "Could not find User: " + name;
						logger.warn(error);
						output.append(error).append("\n");
					}
				}
				// if there is a space, skip it
				if (content.length() != 0 && content.charAt(0) == ' ')
					content = content.substring(1);
			}
			members.forEach(memberID -> output
					.append(Timezones.printUserLocalTime(memberID, event.getGuild())).append("\n"));
			logger.info("Sending Timezones of some Users");
			event.getChannel().sendMessage(output.append("```").toString()).queue();
		}
	}
	
	/**
	 * Will get a full list of all Users with Timezones and reply with all local Times
	 *
	 * @param event Event to get more information
	 */
	public static void getTimezoneOfAllUsersCommand(MessageReceivedEvent event) {
		Map<String, ArrayList<String>> timezoneGroups = new HashMap<>(); // Hashmap with all different timezones
		timezones.forEach((memberID, offset) -> {
			ArrayList<String> members; // Get the correct Arraylist
			if (timezoneGroups.containsKey(offset)) {
				members = timezoneGroups.get(offset);
			} else {
				members = new ArrayList<>();
				timezoneGroups.put(offset, members);
			}
			Member member = event.getGuild().getMemberById(memberID); // Get the Member with the given ID
			if (member != null) {
				members.add(BotEvents.getServerName(member));
			}
		});
		
		Map<Float, String> sortedTimezones = new TreeMap<>(); // sorted Map with all different timezones
		timezoneGroups.forEach((offset, list) -> {
			float offsetNumber; // true numeric Value of the offset for sorting purposes
			if (offset.contains(":")) {
				offsetNumber = Integer.parseInt(offset.substring(0, offset.indexOf(':')))
						+ Integer.parseInt(offset.substring(offset.indexOf(':') + 1)) / 60f;
			} else {
				offsetNumber = Integer.parseInt(offset);
			}
			sortedTimezones.put(offsetNumber, offset);
		});
		
		StringBuilder output = new StringBuilder("```\n");
		for (Map.Entry<Float, String> entry : sortedTimezones.entrySet()) {
			ZonedDateTime localTime = ZonedDateTime.now(ZoneOffset.of(entry.getValue()));
			output.append(localTime.format(sdf))
					.append(" (Z")
					.append(entry.getValue())
					.append("):\n"); // print the Timezone
			timezoneGroups.get(entry.getValue()).forEach(member ->
					output.append("\t")
							.append(member).append("\n")); // print every user in this Timezone
			output.append("\n");
		}
		logger.info("Sending Timezones of all Users");
		event.getChannel().sendMessage(output.append("```").toString()).queue();
	}
	
	/**
	 * Update the Timezone of every User of the Guild
	 *
	 * @param jda The JDA Instance of the Bot
	 */
	static void updateTimezones(JDA jda) { // package-private to be used in Reload.java
		Guild guild = jda.getGuildById(
				BotMain.ROLES.get("Guild")); // get the Guild where the Bot is deployed
		if (guild != null) {
			Map<Long, String> timezonesTemp = new HashMap<>(); // make a new Hashmap that will replace the old one later on
			List<Member> members = guild.getMembers(); // get all Members on the Guild
			members.forEach(member -> {
				User user = member.getUser();
				String nickname = BotEvents.getServerName(member); // get Nickname of User
				nickname = nickname.toLowerCase(Locale.ROOT);
				// ignore Alt account and when the name does not contain a timezone
				if (!user.isBot() && !nickname.contains("[alt") && nickname.contains("[z")) {
					try {
						String offset = getTimezone(nickname.substring(nickname.indexOf("[z")));
						timezonesTemp.put(member.getIdLong(), offset);
						logger.info("Updated Timezone of User: " + BotEvents.getServerName(member));
					} catch (NumberFormatException ignored) { // The timezone in the nickname can't be parsed as a timezone
						// if the User was in the old Map then take his last value as his current
						if (timezones.containsKey(member.getIdLong()))
							timezonesTemp
									.put(member.getIdLong(), timezones.get(member.getIdLong()));
						logger.error("Could not save Timezone of User: " + BotEvents
								.getServerName(member));
					}
				}
			});
			timezones = timezonesTemp; // replace the old Map by the new one
		} else { // The Guild ID given is not a Guild where the Bot is
			logger.error("Bot is not on the specified Server");
		}
	}
	
	/**
	 * Will update the Timezone of a User
	 *
	 * @param userId The ID of the User
	 * @param timezone The new Timezone String of the User in following format: [Z+/-0]
	 *
	 * @return {@code true} if the update was successful or {@code false} if Timezone was not
	 * 		correctly parsed
	 */
	public static boolean updateSpecificTimezone(long userId, String timezone) {
		try {
			timezones.put(userId, getTimezone(timezone)); // try to update the timezone
		} catch (NumberFormatException ignored) {
			return false;
		}
		return true;
	}
	
	/**
	 * Save the Timezones in a File for use after restart
	 */
	public static void saveTimezones() {
		Config.saveTimezones(timezones);
	}
	
	/**
	 * Load the Timezones from the File
	 */
	public static void loadTimezones() {
		timezones = Config.getTimezones();
	}
	
	/**
	 * Return the timezone offset from the String
	 *
	 * @param timezone String with the following format: [Z+/-0]
	 *
	 * @return The offset
	 *
	 * @throws NumberFormatException when the Timezone couldn't be parsed
	 */
	private static String getTimezone(String timezone) throws NumberFormatException {
		try {
			String offset = timezone.substring(timezone.indexOf('z') + 1,
					timezone.indexOf(']')); // extract the offset without extras
			// try to catch as many "troll" offsets as possible
			if (offset.contains("--") || offset.contains("++")) {
				offset = "+" + offset.substring(2);
			} else if (offset.equals("+-0") || offset.equals("-+0") || offset.equals("+/-0")
					|| offset.equals("-/+0") || offset.equals("\u00b10")) {
				offset = "+0";
			} else if (offset.contains(":")) {
				if (offset.indexOf(':') == 2) {
					offset = offset.charAt(0) + "0" + offset.substring(1);
				}
			}
			float offsetNumber;
			if (offset.contains(":")) { // offset is with minutes
				// this is done to confirm that the offset is a valid number
				offsetNumber = Integer.parseInt(offset.substring(0, offset.indexOf(':')))
						+ Integer.parseInt(offset.substring(offset.indexOf(':') + 1)) / 60f;
			} else {
				// this is done to confirm that the offset is a valid number
				offsetNumber = Integer.parseInt(offset);
			}
			if (offsetNumber > 18 || offsetNumber < -18) { // offset is not in range of a timezone
				throw new NumberFormatException();
			}
			return offset;
		} catch (IndexOutOfBoundsException ignored) {
			throw new NumberFormatException();
		}
	}
	
	/**
	 * Returns the String for one User with it's local Time
	 *
	 * @param memberID The Member ID of the User where we want the local time
	 * @param guild The Guild where the Member is
	 *
	 * @return The String that can be send
	 */
	private static String printUserLocalTime(long memberID, Guild guild) {
		Member member = guild.getMemberById(memberID);
		if (member != null) {
			String name = BotEvents.getServerName(member);
			if (timezones.containsKey(memberID)) { // check if we know the timezone of the user
				String timezone = timezones.get(memberID);
				ZonedDateTime localTime = ZonedDateTime.now(ZoneOffset.of(timezone));
				return "It is currently " + localTime.format(sdf) + " (Z" + timezone + ") for "
						+ name;
			} else {
				return name + " does not have a Timezone set.";
			}
		} else {
			logger.error("Could not find user with ID: " + memberID);
			return "";
		}
	}
	
}
