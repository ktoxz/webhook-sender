package dev.ktoxz.pvp.event;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public abstract class PvpEvent {

    protected final String name;
    protected final String description;
    protected static Plugin plugin = null;
    protected final static List<BukkitTask> activeTasks = new ArrayList<>();
    
    public PvpEvent(String name, String description, Plugin plugin) {
        this.name = name;
        this.description = description;
        PvpEvent.plugin = plugin;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // Phương thức trừu tượng mà mỗi sự kiện cụ thể phải triển khai
    public abstract void trigger(Set<Player> players);

    public void onEndMatch() {
        for (BukkitTask task : activeTasks) {
            if (!task.isCancelled()) { // Chỉ hủy nếu chưa bị hủy
                task.cancel();
            }
        }
        activeTasks.clear();
    }
    
    // Utility method to broadcast to players within the arena
    protected void broadcast(Set<Player> players, String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    // Utility method to broadcast action bar message
    protected static void broadcastActionBar(Set<Player> players, String message) {
        String coloredMessage = colorizePrefix(message);
        for (Player player : players) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(coloredMessage));
        }
    }
    
   
    

    // Utility method for coloring messages (copied from old RandomEvent)
    private static String colorizePrefix(String message) {
        if (message.startsWith("[ITEM]")) {
            return "§6" + message; // Vàng sáng
        } else if (message.startsWith("[ENVIRONMENT]")) {
            return "§b" + message; // Xanh dương
        } else if (message.startsWith("[FUN]")) {
            return "§d" + message; // Hồng tím
        } else if (message.startsWith("[BUFF]")) { // Thêm màu cho BUFF/DEBUFF
            return "§a" + message; // Xanh lá
        } else if (message.startsWith("[DEBUFF]")) {
            return "§c" + message; // Đỏ
        } else if(message.startsWith("[SUMMON]")) {
            return "§g" + message; // Vàng
        }
        else {
            return "§f" + message; // Trắng mặc định
        }
    }
}