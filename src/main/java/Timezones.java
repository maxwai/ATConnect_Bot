import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Timezones {

    /**
     * Map with all the Timezones of the Users
     */
    private static Map<Long, Integer> timezones = new HashMap<>();

    /**
     * Update the Timezone of every User of the Guild
     * @param jda The JDA Instance of the Bot
     */
    public static void updateTimezones(JDA jda) {
        Logger logger = LoggerFactory.getLogger("Timezones");
        Guild guild = jda.getGuildById(BotMain.ROLES.get("Guild"));
        if(guild != null) {
            Map<Long, Integer> timezonesTemp = new HashMap<>();
            List<Member> members = guild.getMembers();
            members.forEach(member -> {
                User user = member.getUser();
                String nickname = member.getNickname();
                if(nickname == null)
                    nickname = member.getEffectiveName();
                nickname = nickname.toLowerCase(Locale.ROOT);
                if(!user.isBot() && !nickname.contains("[alt") && nickname.contains("[z")) {
                    try {
                        int offset = getTimezone(nickname.substring(nickname.indexOf("[z")));
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
    public static int getTimezone(String timezone) throws NumberFormatException {
        try {
            String offset = timezone.substring(timezone.indexOf('z') + 1, timezone.indexOf(']'));
            if(offset.contains("--") || offset.contains("++"))
                offset = offset.substring(2);
            else if(offset.equals("+-0") || offset.equals("-+0") ||
                    offset.equals("+/-0") || offset.equals("-/+0") ||
                    offset.equals("±0"))
                offset = "0";
            return Integer.parseInt(offset);
        } catch (IndexOutOfBoundsException ignored) {
            throw new NumberFormatException();
        }
    }

}
