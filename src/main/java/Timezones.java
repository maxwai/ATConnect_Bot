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

    private static Map<Long, Integer> timezones = new HashMap<>();

    public static void updateTimezones(JDA jda) {
        Logger logger = LoggerFactory.getLogger("Timezones");
        Guild guild = jda.getGuildById(BotMain.ROLES.get("Guild"));
        if(guild != null) {
            List<Member> members = guild.getMembers();
            members.forEach(member -> {
                User user = member.getUser();
                String nickname = member.getNickname();
                if(nickname == null)
                    nickname = member.getEffectiveName();
                nickname = nickname.toLowerCase(Locale.ROOT);
                if(!user.isBot() || !nickname.toLowerCase(Locale.ROOT).contains("[alt") || nickname.contains("[z")) {
                    try {
                        int offset = getTimezone(nickname.substring(nickname.indexOf("[z")));
                        timezones.put(member.getIdLong(), offset);
                    } catch (NumberFormatException ignored) {
                        logger.error("Could not save Timezone of User: " + nickname);
                    }
                }
            });
        } else {
            logger.error("Bot is not on the specified Server");
        }
    }

    public static boolean updateSpecificTimezone(long userId, String timezone) {
        return false;
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
