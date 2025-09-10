package config;

import java.io.InputStream;
import java.util.Properties;


public class ConfigAPIKey {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigAPIKey.class.getClassLoader().getResourceAsStream("config.properties")) {


            if (input == null) {
                System.out.println("Lỗi: không tìm thấy file config.properties. Hãy chắc chắn nó nằm trong thư mục 'resources'.");

            }
            properties.load(input);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}