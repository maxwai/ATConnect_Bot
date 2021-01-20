import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                if (emoji.substring(0, emoji.length() - 1).equals("\uD83D\uDDD1") || emoji.equals("\uD83D\uDDD1")) { // :wastebasket:
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
     * Is triggered when the Nickname of a User is changed
     * @param event Nickname Update Event
     */
    @SubscribeEvent
    public void onNicknameChanged(GuildMemberUpdateNicknameEvent event) {
        String newNick = event.getNewNickname();
        if(!event.getUser().isBot() && newNick != null) { // User should not be a Bot
            newNick = newNick.toLowerCase(Locale.ROOT);
            if(newNick.contains("[z") && !newNick.contains("[alt")){ // User should not be an Alt account
                String newOffset = newNick.substring(newNick.indexOf("[z"));
                String oldNick = event.getOldNickname();
                Logger logger = LoggerFactory.getLogger("NicknameChanged");
                if (oldNick != null) {
                    oldNick = oldNick.toLowerCase(Locale.ROOT);
                    String oldOffset = oldNick.substring(oldNick.indexOf("[z"));
                    if (!newOffset.equals(oldOffset)) {
                        if (Timezones.updateSpecificTimezone(event.getUser().getIdLong(), newOffset)) {
                            logger.info("Updated Timezone of User: " + event.getNewNickname());
                        } else {
                            logger.error("Could not save Timezone of User: " + event.getNewNickname());
                        }
                    }
                } else {
                    if (Timezones.updateSpecificTimezone(event.getUser().getIdLong(), newOffset)) {
                        logger.info("Updated Timezone of User: " + event.getNewNickname());
                    } else {
                        logger.error("Could not save Timezone of User: " + event.getNewNickname());
                    }
                }
        }
        }
    }

    /**
     * Is triggered when a Message is written
     * @param event Message Receive Event
     */
    @SubscribeEvent
    public void onReceiveMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String content = event.getMessage().getContentRaw().toLowerCase(Locale.ROOT);
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
        isAdmin = isAdmin || isOwner; // Owner is also admin

        if (content.length() != 0 && content.charAt(0) == '!') {
            LoggerFactory.getLogger("ReceivedMessage").info("Received Message from " + event.getAuthor().getName() + ": " + content);
            String command;
            content = content.substring(1);
            if(content.indexOf(' ') != -1)
                command = content.substring(0, content.indexOf(' '));
            else
                command = content;
            switch (command) {
                case "help" -> { // show Help Page
                    EmbedBuilder eb = EmbedMessages.getHelpPage();
                    if(isEventOrganizer)
                        EmbedMessages.getEventOrganizer(eb); // attach Event Organizer only commands
                    if(isAdmin)
                        EmbedMessages.getAdminHelpPage(eb); // attach Admin only commands
                    channel.sendMessage(eb.build()).queue(BotEvents::addTrashcan);
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
                    if (isAdmin) {
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
                case "reload" -> { // reload the Config files or Timezones
                    if (isAdmin) {
                        if (content.equals("reload")) {
                            channel.sendMessage("Please specify what to reload. Possible is: config, timezones")
                                    .queue(message -> deleteMessageAfterXTime(message, 10));
                        } else {
                            content = content.substring(7);
                            // Reload Config files
                            switch (content) {
                                case "config" -> channel.sendMessage("reloading Config").queue(message -> {
                                    BotMain.reloadConfig();
                                    message.editMessage("Config reloaded successfully").queue();
                                });
                                case "timezones", "timezone" -> // reload Timezones
                                        channel.sendMessage("reloading all user Timezones").queue(message -> {
                                            Timezones.updateTimezones(event.getJDA());
                                            message.editMessage("Timezones reloaded").queue();
                                        });
                                default -> channel.sendMessage("Please specify what to reload. Possible is: config, timezones")
                                        .queue(message -> deleteMessageAfterXTime(message, 10));
                            }
                        }
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

    public static void addTrashcan(Message message) {
        message.addReaction("\uD83D\uDDD1").queue(); // add a reaction to make it easy to delete the post
    }

    public static void deleteMessageAfterXTime(Message message, long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignored) {}
        message.delete().queue(); // delete the Message after X sec
    }

}
