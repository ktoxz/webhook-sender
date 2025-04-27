package dev.ktoxz.commands;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.pvp.PvpSessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Join implements CommandExecutor {

    private final KtoxzWebhook plugin;

    public Join(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }
	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng lệnh này.");
            return true;
        }

        if (!PvpSessionManager.hasActiveSession()) {
            player.sendMessage("§cHiện tại không có phòng PvP nào để tham gia.");
            return true;
        }

        if (!PvpSessionManager.canJoin(player)) {
            player.sendMessage("§cBạn không được mời vào trận PvP này.");
            return true;
        }

        PvpSessionManager.getActiveSession().addPlayer(player);

        // 📢 QUAN TRỌNG: Đăng ký player vào session map
        PvpSessionManager.registerPlayer(player, PvpSessionManager.getActiveSession());

        // Debug in ra toàn bộ player trong phòng
        System.out.println("Danh sách người chơi trong session:");
        for (Player p : PvpSessionManager.getActiveSession().getPlayers()) {
            System.out.println("- " + p.getName());
        }

        PvpSessionManager.getActiveSession().broadcast("§a" + player.getName() + " đã tham gia trận PvP!");

        return true;
    }

}
