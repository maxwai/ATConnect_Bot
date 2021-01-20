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

        eb.addField("`!help`", "shows this page", false);

        eb.setTimestamp(Instant.now());

        return eb;
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
}
