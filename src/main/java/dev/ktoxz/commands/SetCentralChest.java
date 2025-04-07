package dev.ktoxz.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.commands.Constant;
import dev.ktoxz.manager.WebhookManager;
public class SetCentralChest extends JavaPlugin implements CommandExecutor {
	
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	    if (label.equalsIgnoreCase("setcentralchest")) {
	        if (sender instanceof Player player) {
	            Block block = player.getTargetBlockExact(5);
	            if (block != null && block.getType() == Material.CHEST) {
	                Location loc = block.getLocation();
	                getConfig().set("centralChest.world", loc.getWorld().getName());
	                getConfig().set("centralChest.x", loc.getBlockX());
	                getConfig().set("centralChest.y", loc.getBlockY());
	                getConfig().set("centralChest.z", loc.getBlockZ());
	                saveConfig();
	                Constant.set(Constant.CONSTANT.CENTRAL_CHEST_POS, loc);
	                player.sendMessage("§a✔ Rương trung tâm đã được đặt tại: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	                WebhookManager.getInstance().sendWebhook("Rương trung tâm đã được đặt tại: \" + loc.getBlockX() + \", \" + loc.getBlockY() + \", \" + loc.getBlockZ()");
	            } else {
	                player.sendMessage("§c✖ Bạn phải nhìn vào một cái rương để dùng lệnh này.");
	                WebhookManager.getInstance().sendWebhook("Bạn phải nhìn vào một cái rương để dùng lệnh này.");
	            }
	            return true;
	        }
	    }
	    return false;
	}

}
