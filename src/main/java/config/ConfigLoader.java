package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {
    
    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static Properties properties = new Properties();
    
    static {
        try {
            InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            
            if (input == null) {
                logger.warning("config.properties not found in classpath");
            } else {
                properties.load(input);
                logger.info("Config loaded successfully with " + properties.size() + " properties");
                
                properties.keySet().forEach(key -> {
                    String keyStr = key.toString();
                    String value = properties.getProperty(keyStr);
                    
                    if (keyStr.contains("api.key") || keyStr.contains("password")) {
                        value = "***HIDDEN***";
                    }
                    
                    logger.info("Config key: " + keyStr + " = " + value);
                });
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
            return null;
        }
        
        if (value.contains("${")) {
            value = resolveSystemProperties(value);
        }
        
        return value;
    }
   
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null) ? value : defaultValue;
    }
    
   
    private static String resolveSystemProperties(String value) {
        int start = value.indexOf("${");
        while (start != -1) {
            int end = value.indexOf("}", start);
            if (end != -1) {
                String propertyName = value.substring(start + 2, end);
                String propertyValue = System.getProperty(propertyName);
                
                if (propertyValue != null) {
                    value = value.replace("${" + propertyName + "}", propertyValue);
                    logger.fine("Resolved ${" + propertyName + "} to " + propertyValue);
                } else {
                    logger.warning("System property not found: " + propertyName);
                }
            }
            start = value.indexOf("${", end);
        }
        return value;
    }
    
    public static void reload() {
        properties.clear();
        try {
            InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            
            if (input != null) {
                properties.load(input);
                logger.info("Config reloaded successfully");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to reload config.properties", e);
        }
    }
    
 
    public static boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
  
    public static Properties getAllProperties() {
        return new Properties(properties);
    }
}