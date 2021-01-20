import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Timezones {

    /**
     * Map with all the Timezones of the Users
     */
    private static Map<Long, String> timezones = new HashMap<>();
    /**
     * The Logger for Log Messages
     */
    private static final Logger logger = LoggerFactory.getLogger("Timezones");

    /**
     * Update the Timezone of every User of the Guild
     * @param jda The JDA Instance of the Bot
     */
    public static void updateTimezones(JDA jda) {
        Guild guild = jda.getGuildById(BotMain.ROLES.get("Guild"));
        if(guild != null) {
            Map<Long, String> timezonesTemp = new HashMap<>();
            List<Member> members = guild.getMembers();
            members.forEach(member -> {
                User user = member.getUser();
                String nickname = BotEvents.getServerName(member);
                nickname = nickname.toLowerCase(Locale.ROOT);
                if(!user.isBot() && !nickname.contains("[alt") && nickname.contains("[z")) {
                    try {
                        String offset = getTimezone(nickname.substring(nickname.indexOf("[z")));
                        timezonesTemp.put(member.getIdLong(), offset);
                        logger.info("Updated Timezone of User: " + member.getNickname());
                    } catch (NumberFormatException ignored) {
                        if(timezones.containsKey(member.getIdLong()))
                            timezonesTemp.put(member.getIdLong(), timezones.get(member.getIdLong()));
                        logger.error("Could not save Timezone of User: " + member.getNickname());
                    }
                }
            });
            timezones = timezonesTemp;
        } else {
            logger.error("Bot is not on the specified Server");
        }
    }

    /**
     * Will update the Timezone of a User
     * @param userId The ID of the User
     * @param timezone The new Timezone String of the User in following format: [Z+/-0]
     * @return {@code true} if the update was successful or {@code false} if Timezone was not correctly parsed
     */
    public static boolean updateSpecificTimezone(long userId, String timezone) {
        try {
            timezones.put(userId, getTimezone(timezone));
        } catch (NumberFormatException ignored) {
            timezones.remove(userId);
            return false;
        }
        return true;
    }

    public static void saveTimezones() {
        Config.saveTimezones(timezones);
    }

    public static void loadTimezones() {
        timezones = Config.getTimezones();
    }

    /**
     * Return the timezone offset from the String
     * @param timezone String with the following format: [Z+/-0]
     * @return The offset
     * @throws NumberFormatException when the Timezone couldn't be parsed
     */
    private static String getTimezone(String timezone) throws NumberFormatException {
        try {
            String offset = timezone.substring(timezone.indexOf('z') + 1, timezone.indexOf(']'));
            if(offset.contains("--") || offset.contains("++"))
                offset = offset.substring(2);
            else if(offset.equals("+-0") || offset.equals("-+0") ||
                    offset.equals("+/-0") || offset.equals("-/+0") ||
                    offset.equals("±0"))
                offset = "0";
            else if(offset.contains(":")) {
                if(offset.indexOf(':') == 2)
                    offset = offset.charAt(0) + "0" + offset.substring(1);
            }
            float offsetNumber;
            if(offset.contains(":")) {
                offsetNumber = Integer.parseInt(offset.substring(0, offset.indexOf(':'))) + Integer.parseInt(offset.substring(offset.indexOf(':') + 1)) / 60f;
            } else {
                offsetNumber = Integer.parseInt(offset);
            }
            if(offsetNumber > 18 || offsetNumber < -18)
                throw new NumberFormatException();
            return offset; // this is done to confirm that the offset is a valid number
        } catch (IndexOutOfBoundsException ignored) {
            throw new NumberFormatException();
        }
    }

    private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern("EEE HH:mm").withLocale(Locale.ENGLISH);

    public static String printUserLocalTime(long memberID, Guild guild) {
        Member member = guild.getMemberById(memberID);
        if(member != null) {
            String name = BotEvents.getServerName(member);
            if (timezones.containsKey(memberID)) {
                String timezone = timezones.get(memberID);
                ZonedDateTime localTime = ZonedDateTime.now(ZoneOffset.of(timezone));
                return "It is currently " + localTime.format(sdf) + " (Z" + timezone + ") for " + name;
            } else {
                return name + " does not have a Timezone set.";
            }
        } else {
            logger.error("Could not find user with ID: " + memberID);
            return "";
        }
    }

    public static String printAllUsers(Guild guild) {
        Map<String, ArrayList<String>> timezoneGroups = new HashMap<>();
        timezones.forEach((memberID, offset) -> {
            ArrayList<String> members;
            if(timezoneGroups.containsKey(offset)) {
                members = timezoneGroups.get(offset);
            } else {
                members = new ArrayList<>();
                timezoneGroups.put(offset, members);
            }
            Member member = guild.getMemberById(memberID);
            if(member != null) {
                members.add(BotEvents.getServerName(member));
            }
        });

        Map<Float, String> sortedTimezones = new TreeMap<>();
        timezoneGroups.forEach((offset, list) -> {
            float offsetNumber;
            if(offset.contains(":")) {
                offsetNumber = Integer.parseInt(offset.substring(0, offset.indexOf(':'))) + Integer.parseInt(offset.substring(offset.indexOf(':') + 1)) / 60f;
            } else {
                offsetNumber = Integer.parseInt(offset);
            }
            sortedTimezones.put(offsetNumber, offset);
        });

        StringBuilder output = new StringBuilder("```\n");
        for(Map.Entry<Float, String> entry: sortedTimezones.entrySet()) {
            ZonedDateTime localTime = ZonedDateTime.now(ZoneOffset.of(entry.getValue()));
            output.append(localTime.format(sdf)).append(" (Z").append(entry.getValue()).append("):\n");
            timezoneGroups.get(entry.getValue()).forEach(member -> output.append("\t").append(member).append("\n"));
            output.append("\n");
        }
        return output.append("```").toString();
    }

}
