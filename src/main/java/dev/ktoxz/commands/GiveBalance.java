package dev.ktoxz.commands;

import dev.ktoxz.manager.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GiveBalance implements CommandExecutor {

    private final JavaPlugin plugin;

    public GiveBalance(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givebalance <player> <amount>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§cKhông tìm thấy người chơi.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cSố lượng không hợp lệ.");
            return true;
        }

        // ✅ Thực hiện cập nhật MongoDB trong async task
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int re = UserManager.insertBalance(target, amount);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (re == 1) {
                    sender.sendMessage("§a✔ Đã cộng " + amount + " coin cho " + target.getName());
                    target.sendMessage("§a Nhận "+ amount + " coin từ "+sender.getName());
                } else {
                    sender.sendMessage("§c❌ Không thể cộng coin. Có thể người chơi chưa có hồ sơ trong DB.");
                }
            });
        });

        return true;
    }
}
