package commands;

import bot.BotEvents;
import telegram.TelegramLogger;
import java.awt.Color;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

public class Help {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final TelegramLogger logger = TelegramLogger.getLogger("Help Command");
	
	/**
	 * will show the Help Embed to the User
	 *
	 * @param isInstructor If this User is an Instructor
	 * @param isEventOrganizer If this User is an Event Organizer
	 * @param isAdmin If this User is an Admin
	 * @param channel The Channel where the Command was send
	 * @param content The Content of the Message that was send.
	 */
	public static void showHelp(boolean isInstructor, boolean isEventOrganizer, boolean isAdmin,
			MessageChannel channel, String content) {
		if (content.contains("event")) {
			if (isEventOrganizer) {
				sendEventHelpPage(channel);
			}
		} else {
			EmbedBuilder eb = getHelpPage(); // get the Basic Help Page
			if (isInstructor)
				getInstructorHelpPage(eb); // attach Instructor only commands
			if (isEventOrganizer)
				getEventOrganizerHelpPage(eb); // attach Event Organizer only commands
			if (isAdmin)
				getAdminHelpPage(eb); // attach Admin only commands
			logger.info("Sending Help page");
			channel.sendMessage(eb.build()).queue(BotEvents::addTrashcan);
		}
	}
	
	public static void sendEventHelpPage(MessageChannel channel) {
		EmbedBuilder eb = getEventPage();
		logger.info("Sending Event Help Page");
		channel.sendMessage(eb.build()).queue(BotEvents::addTrashcan);
	}
	
	private static EmbedBuilder getEventPage() {
		EmbedBuilder eb = new EmbedBuilder();
		
		eb.setColor(Color.YELLOW);
		
		eb.setTitle("Event Commands:");
		
		eb.setDescription("List of some Event Commands");
		
		eb.addField("More to come", "empty for now", false);
		
		eb.setTimestamp(Instant.now());
		
		return eb;
	}
	
	/**
	 * Build the basic Help Page
	 *
	 * @return The Basic Help Page, still needs to be Build
	 */
	private static EmbedBuilder getHelpPage() {
		EmbedBuilder eb = new EmbedBuilder();
		
		eb.setColor(Color.YELLOW);
		
		eb.setTitle("Commands:");
		
		eb.setDescription("List of all known Commands");
		
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
	 * Adds the Instructor Portion to the Help Page
	 *
	 * @param eb the already pre-build Help Page
	 */
	private static void getInstructorHelpPage(EmbedBuilder eb) {
		eb.appendDescription(" with commands for Instructors");
		
		eb.addBlankField(false);
		
		eb.addField("Instructor Commands", "Commands that are only for Instructors",
				false);
		
		eb.addField("`!trained`", """
				Will add the Role `Trained` the mentioned Member.
				The mentioned Member must be mentioned with a Tag.
				Only one Member at a time can be mentioned with the Command.
				Syntax:
				`!trained @User`""", false);
	}
	
	/**
	 * Adds the Event Organizer Portion to the Help Page
	 *
	 * @param eb the already pre-build Help Page
	 */
	private static void getEventOrganizerHelpPage(EmbedBuilder eb) {
		if (eb.getDescriptionBuilder().toString().contains("with commands for"))
			eb.appendDescription(" and Event Organizers");
		else
			eb.appendDescription(" with commands for Event Organizers");
		
		eb.addBlankField(false);
		
		eb.addField("Event Organizer Commands",
				"Commands that are only for event organizers", false);
		
		eb.addField("`!countdown`", """
				adds a countdown to the next event in the welcome channel
				Syntax:
				`!countdown DD.MM.YYYY HH:mm <additional Text>`
				The time is always in UTC""", true);
		
		eb.addField("`!event create`", "Will begin the creation of a new event", true);
	}
	
	/**
	 * Adds the Admin Portion to the Help page
	 *
	 * @param eb the already pre-build Help Page
	 */
	private static void getAdminHelpPage(EmbedBuilder eb) {
		if (eb.getDescriptionBuilder().toString().contains("with commands for"))
			eb.appendDescription(" and Admins");
		else
			eb.appendDescription(" with commands for Admins");
		
		eb.addBlankField(false);
		
		eb.addField("Admin Commands", "Commands that are only for admins:",
				false);
		
		eb.addField("`!restart`", "restarts the bot", true);
		
		// This Command should not be shown since only the Owner can do it.
		// eb.addField("!stop", "stops the bot", true);
		
		eb.addField("`!reload XY`", """
				reloads all config files
				Following arguments are available:
				`config`, `timezones`""", true);
		
		// This Command should not be shown since only the Owner can do it.

//        eb.addField("`!purge`", """
//                purges the given amount of Messages from the channel not including the command.
//                Layout:
//                `!purge 10`
//                `!purge all`""", true);
	}
}
