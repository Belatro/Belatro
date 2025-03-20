package backend.belatro.util;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvInitializer {

    public static void init() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()  // Continue even if the .env file doesn't exist
                .load();
        dotenv.entries().forEach(entry -> {
            // Only set the system property if it hasn't been set yet
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }
}
