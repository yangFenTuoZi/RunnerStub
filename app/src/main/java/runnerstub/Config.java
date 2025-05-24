package runnerstub;

import android.content.Context;
import android.content.res.AssetManager;

import java.util.Properties;

public final class Config {
    public static String COMMAND;
    public static boolean REDUCE_PERM;
    public static String TARGET_PERM;

    public static void load(AssetManager assets) {
        try {
            var inputStream = assets.open("config.prop");
            Properties properties = new Properties();
            properties.load(inputStream);
            inputStream.close();

            COMMAND = properties.getProperty("command", "");
            REDUCE_PERM = Boolean.parseBoolean(properties.getProperty("reduce_perm", "false"));
            TARGET_PERM = properties.getProperty("target_perm", "");
        } catch (Exception e) {
            throw new RuntimeException("Config: load: ", e);
        }
    }
}
