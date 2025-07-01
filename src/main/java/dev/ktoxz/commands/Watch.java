package dev.ktoxz.commands;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.pvp.PvpSessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Watch implements CommandExecutor {

	
    private final KtoxzWebhook plugin;

    public Watch(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng lệnh này.");
            return true;
        }

        if (!PvpSessionManager.hasActiveSession()) {
            player.sendMessage("§cKhông có phòng PvP nào đang bắt đầu để xem");
            return true;
        }

        PvpSessionManager.getActiveSession().teleportToWatch(player);
        return true;
    }
}
