import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final String TOKEN_FILE_NAME = "Token.cfg";
    private static final String ROLES_FILE_NAME = "Roles.cfg";

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
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded Token");
        return token;
    }

    public static Map<String, Long> getRoles() {
        Logger logger = LoggerFactory.getLogger("TokenGrabber");
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
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.info("Loaded Roles");
        return roles;
    }

}
