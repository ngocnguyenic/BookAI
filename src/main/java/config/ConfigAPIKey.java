package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigAPIKey {
    
    private static final Logger logger = Logger.getLogger(ConfigAPIKey.class.getName());
    private static Properties properties = new Properties();
    
    static {
        try {
        
            InputStream input = ConfigAPIKey.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            
            if (input == null) {
                logger.warning("config.properties not found in classpath");
            } else {
                properties.load(input);
                logger.info("Config loaded successfully with " + properties.size() + " properties");
                
                // Debug: In ra tất cả keys
                properties.keySet().forEach(key -> 
                    logger.info("Config key: " + key + " = " + properties.getProperty(key.toString()))
                );
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load config.properties", e);
        }
    }
    
    public static String getProperty(String key) {
        String value = properties.getProperty(key);
        
        if (value == null) {
            logger.warning("Property '" + key + "' not found in config");
            logger.info("Available keys: " + properties.keySet());
        }
        
        return value;
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}