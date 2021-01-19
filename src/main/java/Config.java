import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Config {

    private static final String TOKEN_FILE_NAME = "Token.cfg";
    private static final String ROLES_FILE_NAME = "Roles.cfg";
    private static final String COUNTDOWN_FILE_NAME = "Countdowns.cfg";

    public static String getToken() {
        Logger logger = LoggerFactory.getLogger("TokenGrabber");
        String token = "";
        try {
            File tokenFile = new File(TOKEN_FILE_NAME);
            if (tokenFile.createNewFile()) {
                logger.warn("Created " + TOKEN_FILE_NAME + " at " + tokenFile.getAbsolutePath());
                logger.error("No Token available. Add Token in " + TOKEN_FILE_NAME);
                throw new RuntimeException("No Token available");
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

    public static Map<String, Long> getRoles() {
        Logger logger = LoggerFactory.getLogger("RolesGrabber");
        Map<String, Long> roles = new HashMap<>();
        try {
            File rolesFile = new File(ROLES_FILE_NAME);
            if (rolesFile.createNewFile()) {
                logger.warn("Created " + ROLES_FILE_NAME + " at " + rolesFile.getAbsolutePath());
                logger.error("No Roles available. Add Roles in " + ROLES_FILE_NAME);
                throw new RuntimeException("No Roles available");
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

}
