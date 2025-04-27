package dev.ktoxz.commands;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.pvp.PvpSessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Start implements CommandExecutor {

	
    private final KtoxzWebhook plugin;

    public Start(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng lệnh này.");
            return true;
        }

        if (!PvpSessionManager.hasActiveSession()) {
            player.sendMessage("§cKhông có phòng PvP nào để bắt đầu.");
            return true;
        }

        if (!PvpSessionManager.isOwner(player)) {
            player.sendMessage("§cChỉ chủ phòng mới có thể bắt đầu trận PvP.");
            return true;
        }

        // Bắt đầu PvP!
        if(PvpSessionManager.getActiveSession().getPlayers().size() == 1) {
        	PvpSessionManager.getActiveSession().broadcast("§cKhông bắt đầu được do mới có 1 người :v");
        	return true;
        }
        
        PvpSessionManager.getActiveSession().broadcast("§6Trận PvP bắt đầu!");

        // TODO: Sau đây sẽ thực hiện backup, spawn chest, countdown, start PvP như ta đã làm ở test session.

        PvpSessionManager.getActiveSession().setStarted(true);
        
        PvpSessionManager.getActiveSession().preparePlayersForBattle();
        PvpSessionManager.getActiveSession().startCountdown();
        return true;
    }
}
