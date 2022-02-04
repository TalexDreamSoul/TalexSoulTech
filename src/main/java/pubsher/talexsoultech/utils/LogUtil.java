package pubsher.talexsoultech.utils;

import org.bukkit.Bukkit;

public class LogUtil {

    public static void log(String message) {

        Bukkit.getServer().getConsoleSender().sendMessage(message);

    }

}
