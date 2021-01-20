import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Config {

    /**
     * The File name of the TOKEN Config file
     */
    private static final String TOKEN_FILE_NAME = "Token.cfg";
    /**
     * The File name of the ROLES Config file
     */
    private static final String ROLES_FILE_NAME = "Roles.cfg";
    /**
     * The File name where the Countdowns are saved
     */
    private static final String COUNTDOWN_FILE_NAME = "Countdowns.cfg";
    /**
     * The File name where the Timezones are saved
     */
    private static final String TIMEZONE_FILE_NAME = "Timezones.cfg";

    /**
     * Get the Token of the Bot.
     * @return The Token of the Bot
     */
    public static String getToken() {
        Logger logger = LoggerFactory.getLogger("TokenGrabber");
        String token = "";
        try {
            File tokenFile = new File(TOKEN_FILE_NAME);
            if (tokenFile.createNewFile()) {
                logger.warn("Created " + TOKEN_FILE_NAME + " at " + tokenFile.getAbsolutePath());
                logger.error("No Token available. Add Token in " + TOKEN_FILE_NAME);
                throw new RuntimeException("No Token available"); // finish the Program since no Token present
            }
            BufferedReader reader = new BufferedReader(new FileReader(tokenFile));
            token = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded Token");
        return token;
    }

    /**
     * Get the Role IDs of the important Roles
     * @return Map with all IDs
     */
    public static Map<String, Long> getRoles() {
        Logger logger = LoggerFactory.getLogger("RolesGrabber");
        Map<String, Long> roles = new HashMap<>();
        try {
            File rolesFile = new File(ROLES_FILE_NAME);
            if (rolesFile.createNewFile()) {
                logger.warn("Created " + ROLES_FILE_NAME + " at " + rolesFile.getAbsolutePath());
                logger.error("No Roles available. Add Roles in " + ROLES_FILE_NAME);
                throw new RuntimeException("No Roles available"); // finish the Program since no Roles present
            }
            BufferedReader reader = new BufferedReader(new FileReader(rolesFile));
            String line = reader.readLine();
            while (line != null) {
                roles.put(line.substring(0, line.indexOf('=')), Long.valueOf(line.substring(line.indexOf('=') + 1)));
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded Roles");
        return roles;
    }

    /**
     * Get the saved Countdowns
     * @return ArrayList with the String Arrays with the informations
     */
    public static ArrayList<String[]> getCountdowns() {
        Logger logger = LoggerFactory.getLogger("CountdownGrabber");
        ArrayList<String[]> countdowns = new ArrayList<>();
        try {
            File countdownFile = new File(COUNTDOWN_FILE_NAME);
            if (countdownFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(countdownFile));
                String line = reader.readLine();
                while (line != null) {
                    countdowns.add(line.split(","));
                    line = reader.readLine();
                }
                reader.close();
            } else {
                logger.warn("No Countdowns saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded " + countdowns.size() + " Countdowns");
        return countdowns;
    }

    /**
     * Save the Countdowns
     * @param countdowns The Infos of the Countdowns
     */
    public static void saveCountdowns(Stack<Countdowns.CountdownsThread> countdowns) {
        Logger logger = LoggerFactory.getLogger("CountdownSaver");
        try {
            File countdownFile = new File(COUNTDOWN_FILE_NAME);
            PrintWriter writer = new PrintWriter(countdownFile);
            countdowns.forEach(countdownThread -> {
                String[] infos = countdownThread.getInfos();
                writer.write(infos[0] + "," + infos[1] + "," + infos[2] + "," + infos[3] + "\n");
            });
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Saved Countdowns");
    }

    /**
     * Get the User Timezones
     * @return Map with the Timezones of the Users
     */
    public static Map<Long, Integer> getTimezones() {
        Logger logger = LoggerFactory.getLogger("CountdownGrabber");
        Map<Long, Integer> timezones = new HashMap<>();
        try {
            File timezoneFile = new File(TIMEZONE_FILE_NAME);
            if (timezoneFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(timezoneFile));
                String line = reader.readLine();
                while (line != null) {
                    timezones.put(Long.valueOf(line.substring(0, line.indexOf('='))),
                            Integer.valueOf(line.substring(line.indexOf('=') + 1)));
                    line = reader.readLine();
                }
                reader.close();
            } else {
                logger.warn("No Timezone saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded Timezones");
        return timezones;
    }

    public static void saveTimezones(Map<Long, Integer> timezones) {
        Logger logger = LoggerFactory.getLogger("TimezoneSaver");
        try {
            File countdownFile = new File(TIMEZONE_FILE_NAME);
            PrintWriter writer = new PrintWriter(countdownFile);
            timezones.forEach((user, timezone) -> writer.write(user + "=" + timezone + "\n"));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Saved Timezones");
    }

}
