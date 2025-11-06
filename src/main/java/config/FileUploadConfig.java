package config;

import java.io.File;
import java.util.logging.Logger;

public class FileUploadConfig {
    
    private static final Logger logger = Logger.getLogger(FileUploadConfig.class.getName());
    private static String uploadPath = null;
   
    public static String getUploadPath() {
        if (uploadPath == null) {
            initUploadPath();
        }
        return uploadPath;
    }
    
    private static void initUploadPath() {
       
        uploadPath = System.getProperty("app.upload.dir");
        
       
        if (uploadPath == null || uploadPath.trim().isEmpty()) {
            uploadPath = ConfigLoader.getProperty("upload.directory");
        }
        
        
        if (uploadPath == null || uploadPath.trim().isEmpty()) {
            String userHome = System.getProperty("user.home");
            uploadPath = userHome + File.separator + "BookAI" + File.separator + "uploads";
            logger.info("Using default upload path: " + uploadPath);
        } else {
            logger.info("Using configured upload path: " + uploadPath);
        }
        
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (created) {
                logger.info("✅ Created upload directory: " + uploadPath);
            } else {
                logger.severe("❌ Failed to create upload directory: " + uploadPath);
            }
        } else {
            logger.info("✅ Upload directory exists: " + uploadPath);
        }
        if (!uploadDir.canWrite()) {
            logger.severe("❌ Upload directory is not writable: " + uploadPath);
        }
    }
    
    public static File getUploadDir() {
        return new File(getUploadPath());
    }

    public static void reset() {
        uploadPath = null;
    }
    
    public static boolean isValid() {
        File dir = getUploadDir();
        return dir.exists() && dir.isDirectory() && dir.canWrite();
    }
}