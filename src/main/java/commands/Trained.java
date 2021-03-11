package commands;

import bot.BotEvents;
import bot.BotMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class Trained {
	
	/**
	 * The Logger for Log Messages
	 */
	private static final Logger logger = LoggerFactory.getLogger("Trained Command");
	
	/**
	 * Will add the Role "Trained" to a User
	 *
	 * @param isInstructor If the User sending the Command is an Instructor
	 * @param event Event to get more information
	 */
	public static void makeUserTrained(boolean isInstructor, MessageReceivedEvent event) {
		if (isInstructor) { // only the Instructor can mark Users as trained
			MessageChannel channel = event.getChannel();
			String errorMessage = null; // If there was an error message send to the User, this will then send a Log as well
			List<Member> member = event.getMessage().getMentionedMembers();
			if (member.size() == 1) {
				Role trainedRole = event.getGuild().getRoleById(BotMain.ROLES.get("Trained"));
				if (trainedRole != null) {
					try {
						event.getGuild().addRoleToMember(member.get(0), trainedRole)
								.queue(); // add the role to the User
						MessageBuilder messageBuilder = new MessageBuilder();
						messageBuilder.append(member.get(0)).append(" has the Role `Trained`");
						channel.sendMessage(messageBuilder.build())
								.queue(); // confirm to the User that the role was added
						logger.info(
								"Added Trained Role to " + BotEvents.getServerName(member.get(0)));
					} catch (HierarchyException e) { // The Trained Role is above the Bot role
						errorMessage = "Could not add the Role to User because Bot has his Role under the Trained role";
						channel.sendMessage(errorMessage).queue(BotEvents::addTrashcan);
					} catch (InsufficientPermissionException e) { // The Bot doesn't have permissions to add Roles
						errorMessage = "Bot doesn't have the Permission Manage Roles";
						channel.sendMessage(errorMessage).queue(BotEvents::addTrashcan);
					}
				} else { // The Trained Role ID is wrong in the config
					errorMessage = "Can't find the Trained Role. Please update the role ID's";
					channel.sendMessage(errorMessage).queue(BotEvents::addTrashcan);
				}
			} else if (member.size() == 0) { // No members mentioned
				channel.sendMessage("You have to mention a User that is Trained").queue();
			} else { // Too many members mentioned
				channel.sendMessage(
						"Can't mention multiple members at once, please mention one member at a time")
						.queue(message -> BotEvents.deleteMessageAfterXTime(message, 10));
			}
			if (errorMessage != null) {
				logger.error(errorMessage); // There was an Error so send it as well in the Log
			}
		} else // User isn't an Instructor
			event.getChannel().sendMessage("You don't have permission for this command").queue();
	}
}
