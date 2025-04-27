package dev.ktoxz.commands;


import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.pvp.PvpSessionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Dual implements CommandExecutor {

    private final KtoxzWebhook plugin;

    public Dual(KtoxzWebhook plugin) {
        this.plugin = plugin;

    }

	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng lệnh này.");
            return true;
        }

        if (PvpSessionManager.hasActiveSession()) {
            player.sendMessage("§cĐã có trận PvP đang diễn ra. Vui lòng chờ.");
            return true;
        }

        if (args.length == 0) {
            // Mở room public
            PvpSessionManager.createSession(player, true);
            Bukkit.broadcastMessage("§e[!] §f" + player.getName() + " vừa mở phòng PvP tự do! Gõ §b/join §fđể tham gia.");
        } else {
            // Mở room private, mời từng player
            PvpSessionManager.createSession(player, false);

            for (String targetName : args) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    PvpSessionManager.getActiveSession().addInvited(target);
                    player.sendMessage("§aĐã mời " + target.getName() + " tham gia PvP!");
                    target.sendMessage("§eBạn được mời PvP bởi " + player.getName() + "! Gõ §b/join §eđể tham gia.");
                } else {
                    player.sendMessage("§cKhông tìm thấy người chơi: " + targetName);
                }
            }
        }

        return true;
    }
}