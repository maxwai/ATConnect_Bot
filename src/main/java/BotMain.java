import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Map;

public class BotMain {

    public static String TOKEN = Config.getToken();
    public static Map<String, Long> ROLES = Config.getRoles();

    public static final long[] restartIDs = new long[2];
    public static final Object lock = new Object();

    private static JDA jda;
    private static JDABuilder jdaBuilder;

    public static void main(String[] args) throws LoginException {
        initializeJDABuilder();
        connectBot();
    }

    private static void initializeJDABuilder() {
        jdaBuilder = JDABuilder.createDefault(TOKEN)
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(new BotEvents())
                .setActivity(Activity.listening("!help"))
                .disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING)
                .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);
    }

    public static void reloadConfig() {
        TOKEN = Config.getToken();
        ROLES = Config.getRoles();
    }

    public static void restartBot(){
        disconnectBot();
        try {
            connectBot();
        } catch (LoginException e) {
            e.printStackTrace();
        }
        synchronized (lock) {
            while (restartIDs[1] == 0L) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
            TextChannel channel = jda.getTextChannelById(restartIDs[0]);
            if (channel != null)
                channel.editMessageById(restartIDs[1], "Bot successfully restarted").queue();
            restartIDs[0] = 0;
            restartIDs[1] = 0;
        }
    }

    private static void connectBot() throws LoginException {
        jda = jdaBuilder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException ignored) {
        }
    }

    public static void disconnectBot() {
        Countdowns.closeAllThreads();
        jda.getRegisteredListeners().forEach(jda::removeEventListener);
        jda.shutdown();
    }

}
