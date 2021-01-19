import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.Map;

public class BotMain {

    /**
     * Token of the Bot, Fetch with the Token.cfg file
     */
    public static String TOKEN = Config.getToken();
    /**
     * Map of special Role IDs:
     * Admin, Owner, Event Organizer
     */
    public static Map<String, Long> ROLES = Config.getRoles();

    /**
     * The IDs for when the Bot is restarted to know which Message to edit
     */
    public static final long[] restartIDs = new long[2];
    /**
     * Lock for the {@link #restartIDs}
     */
    public static final Object lock = new Object();

    /**
     * {@link JDA} Instance of the Bot
     */
    private static JDA jda;
    /**
     * {@link JDABuilder} for the Bot
     */
    private static JDABuilder jdaBuilder;

    public static void main(String[] args) throws LoginException {
        initializeJDABuilder();
        connectBot();
    }

    /**
     * Will setup the JDA Builder with the necessary settings
     */
    private static void initializeJDABuilder() {
        jdaBuilder = JDABuilder.createDefault(TOKEN)
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(new BotEvents())
                .setActivity(Activity.listening("!help"))
                .disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING)
                .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);
    }

    /**
     * Will reload the Configs (without the Countdowns)
     */
    public static void reloadConfig() {
        TOKEN = Config.getToken();
        ROLES = Config.getRoles();
    }

    /**
     * Will restart the Bot
     */
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

    /**
     * Connect to the Bot and load the Countdowns
     * @throws LoginException if the TOKEN of the Bot is wrong
     */
    private static void connectBot() throws LoginException {
        jda = jdaBuilder.build();
        try {
            jda.awaitReady();
            Countdowns.restartCountdowns(jda);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Will disconnect the Bot and save the Countdowns
     */
    public static void disconnectBot() {
        Countdowns.closeAllThreads();
        jda.getRegisteredListeners().forEach(jda::removeEventListener);
        jda.shutdown();
    }

}
