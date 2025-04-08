package dev.ktoxz.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import dev.ktoxz.main.KtoxzWebhook;

public class SetCentralChest implements CommandExecutor {

    private final KtoxzWebhook plugin;

    public SetCentralChest(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Lệnh này chỉ dùng cho người chơi.");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() != Material.CHEST) {
            player.sendMessage("§cBạn cần nhìn vào một cái rương.");
            return true;
        }

        Location loc = target.getLocation();
        plugin.getConfig().set("central-chest", loc.serialize());
        plugin.saveConfig();

        player.sendMessage("§a✔ Rương trung tâm đã được đặt tại: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        plugin.getLogger().info("📦 Rương trung tâm đã được lưu: " + loc.toString());
        return true;
    }
}
