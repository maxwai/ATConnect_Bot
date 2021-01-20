import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;

public class EmbedMessages {

    /**
     * Build the basic Help Page
     * @return The Basic Help Page, still needs to be Build
     */
    public static EmbedBuilder getHelpPage() {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(Color.YELLOW);

        eb.setTitle("Commands:");

        eb.setDescription("List off all known Commands");

        eb.addField("`!help`", "shows this page", true);

        eb.addField("`!timezones`", "Lists all Timezones with their Users", true);

        eb.addField("`!time`", """
                Will show the local time of the given Users.
                The Time is calculated using the Zulu offset of the Nickname.
                Users can be given as Tags or as a `,` separated List of Names.
                When Names are given, only a partial match is necessary.
                Example layouts:
                `!time User, User1`
                `!time @User @User2`""", false);

        eb.setTimestamp(Instant.now());

        return eb;
    }

    /**
     * Adds the Event Organizer Portion to the Help Page
     * @param eb the already pre-build Help Page
     */
    public static void getEventOrganizer(EmbedBuilder eb) {
        eb.setDescription(eb.getDescriptionBuilder().append(" with commands for Event Organizers"));

        eb.addBlankField(false);

        eb.addField("Event Organizer Commands", "Commands that are only for event organizers", false);

        eb.addField("`!countdown`", """
                adds a countdown to the next event in the welcome channel
                Syntax:
                `!countdown DD.MM.YYYY HH:mm <additional Text>`
                The time is always in UTC""", false);
    }

    /**
     * Adds the Admin Portion to the Help page
     * @param eb the already pre-build Help Page
     */
    public static void getAdminHelpPage(EmbedBuilder eb) {
        StringBuilder desc = eb.getDescriptionBuilder();
        if(desc.toString().contains("with commands for"))
            eb.setDescription(desc.append(" and Admins"));
        else
            eb.setDescription(desc.append(" with commands for Admins"));

        eb.addBlankField(false);

        eb.addField("Admin Commands", "Commands that are only for admins:", false);

        eb.addField("`!restart`", "restarts the bot", true);

        //eb.addField("!stop", "stops the bot", true);

        eb.addField("`!reload XY`", """
                reloads all config files
                Following arguments are available:
                `config`, `timezones`""", true);

//        eb.addField("`!purge`", """
//                purges the given amount of Messages from the channel not including the command.
//                Layout:
//                `!purge 10`
//                `!purge all`""", true);
    }
}
