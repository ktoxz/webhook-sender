package dev.ktoxz.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.manager.UserManager;
import dev.ktoxz.model.PendingPayRequest;

public class AcceptPay implements CommandExecutor {

    private final JavaPlugin plugin;

    public AcceptPay(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player receiver)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        PendingPayRequest request = Pay.getPendingPayments().remove(receiver.getUniqueId());
        if (request == null) {
            receiver.sendMessage("§cBạn không có yêu cầu chuyển tiền nào.");
            return true;
        }

        Player senderPlayer = request.getSender();
        double amount = request.getAmount();

        // Cộng tiền
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UserManager.insertBalance(receiver, (int) amount);

            // Gửi thông báo
            receiver.sendMessage("§a✔ Bạn đã nhận " + amount + " xu từ " + senderPlayer.getName());
            senderPlayer.sendMessage("§a✔ " + receiver.getName() + " đã nhận " + amount + " xu từ bạn.");
        });

        return true;
    }
}
