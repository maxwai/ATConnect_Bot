import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BotEvents {

    /**
     * Is triggered when an Emote is added / count is changed
     * @param event Reaction add Event
     */
    @SubscribeEvent
    public void onEmoteAdded(GuildMessageReactionAddEvent event) {
        if(event.getUser().isBot()) return; // don't react if the bot is adding this emote
        event.retrieveMessage().queue(message -> {
            if(!message.getAuthor().isBot()) return; // delete only bot messages
            if(event.getReactionEmote().isEmoji()) {
                String emoji = event.getReactionEmote().getEmoji();
                if (emoji.substring(0, emoji.length() - 1).equals("\uD83D\uDDD1")) { // :wastebasket:
                    LoggerFactory.getLogger("ReactionAdded").info("deleting message because of :wastebasket: reaction");
                    if(Countdowns.messageIds.contains(message.getId())) {
                        Countdowns.closeSpecificThread(message.getId());
                    }
                    message.delete().queue();
                }
            }
        });
    }

    /**
     * Is triggered when a Message is written
     * @param event Message Receive Event
     */
    @SubscribeEvent
    public void onReceiveMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();

        // These boolean are always false if written in a personal message
        boolean isAdmin = false;
        boolean isEventOrganizer = false;

        // if this message is from a Guild/Server then check if they have the Admin and/or event organizer role
        if(event.isFromGuild()) {
            Member authorMember = event.getGuild().getMember(event.getAuthor());
            List<Role> rolesOfUser = authorMember != null ? authorMember.getRoles() : new ArrayList<>();
            isAdmin = rolesOfUser.contains(event.getGuild().getRoleById(BotMain.ROLES.get("Admin")));
            isEventOrganizer = rolesOfUser.contains(event.getGuild().getRoleById(BotMain.ROLES.get("Event_Organizer")));
        }
        boolean isOwner = event.getAuthor().getIdLong() == BotMain.ROLES.get("Owner");

        if (content.length() != 0 && content.charAt(0) == '!') {
            LoggerFactory.getLogger("ReceivedMessage").info("Received Message from " + event.getAuthor().getName() + ": " + content);
            if(content.indexOf(' ') != -1)
                content = content.substring(1, content.indexOf(' '));
            else
                content = content.substring(1);
            switch (content) {
                case "help" -> { // show Help Page
                    EmbedBuilder eb = EmbedMessages.getHelpPage();
                    if(isEventOrganizer)
                        EmbedMessages.getEventOrganizer(eb); // attach Event Organizer only commands
                    if(isAdmin)
                        EmbedMessages.getAdminHelpPage(eb); // attach Admin only commands
                    channel.sendMessage(eb.build()).queue();
                }
                case "ping" -> { // make a ping test
                    long time = System.currentTimeMillis();
                    channel.sendMessage("Pong!").queue(message ->
                        message.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue());
                }
                case "countdown" -> { // create a live countdown
                    if (isEventOrganizer || isOwner) {
                        Countdowns.startNewCountdown(event);
                    } else
                        channel.sendMessage("You don't have permission for this command").queue();
                }
                case "restart" -> { // restarts the Bot connection
                    if (isAdmin || isOwner) {
                        BotMain.restartIDs[0] = channel.getIdLong();
                        channel.sendMessage("restarting Bot").queue(message -> {
                            synchronized (BotMain.lock) {
                                BotMain.restartIDs[1] = message.getIdLong();
                                BotMain.lock.notify();
                            }
                        });
                        BotMain.restartBot();
                    } else
                        channel.sendMessage("You don't have permission for this command").queue();
                }
                case "reload" -> { // reload the Config files
                    if (isOwner || isAdmin) {
                        channel.sendMessage("reloading Config").queue(message -> {
                            BotMain.reloadConfig();
                            message.editMessage("Config reloaded successfully").queue();
                        });
                    } else
                        channel.sendMessage("You don't have permission for this command").queue();
                }
                case "stop" -> { // stops the Bot, this takes a while
                    if(isOwner) {
                        channel.sendMessage("stopping the Bot. Bye...").queue();
                        BotMain.disconnectBot();
                    } else
                        channel.sendMessage("You don't have permission for this command").queue();
                }
            }
        }
    }

}
