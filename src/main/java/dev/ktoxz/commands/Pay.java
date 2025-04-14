package dev.ktoxz.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.manager.EffectManager;
import dev.ktoxz.manager.UserManager;
import dev.ktoxz.model.PendingPayRequest;

public class Pay implements CommandExecutor {

    private final JavaPlugin plugin;
    private static final Map<UUID, PendingPayRequest> pendingPayments = new HashMap<>();

    public Pay(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player sendPlayer)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }

        Player receiver = Bukkit.getPlayerExact(args[0]);
        if (receiver == null || receiver.equals(sendPlayer)) {
            sender.sendMessage("§cKhông tìm thấy người chơi hoặc bạn đang gửi cho chính mình.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sendPlayer.sendMessage("§cSố tiền phải lớn hơn 0.");
                return true;
            }
        } catch (NumberFormatException e) {
            sendPlayer.sendMessage("§cSố lượng không hợp lệ.");
            return true;
        }

        // Kiểm tra số dư
        UserManager.getBalanceAsync(sendPlayer, balance -> {
            if (balance < amount) {
                sendPlayer.sendMessage("§cBạn không đủ tiền.");
                return;
            }

            // Trừ tiền người gửi
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                UserManager.insertBalance(sendPlayer, (int) -amount);
                pendingPayments.put(receiver.getUniqueId(), new PendingPayRequest(sendPlayer, amount));

                // Gửi thông báo
                receiver.sendMessage("§e" + sendPlayer.getName() + " muốn gửi bạn §a" + amount + " xu.");
                receiver.sendMessage("§7Gõ §a/ac §7để nhận trong vòng 60 giây.");

                sendPlayer.sendMessage("§a✔ Yêu cầu chuyển tiền đã gửi. Đợi người nhận xác nhận.");

                // Hẹn giờ huỷ nếu không nhận sau 60s
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (pendingPayments.containsKey(receiver.getUniqueId())) {
                        // Hoàn tiền
                        UserManager.insertBalance(sendPlayer, (int) amount);
                        pendingPayments.remove(receiver.getUniqueId());
                        sendPlayer.sendMessage("§c❌ Người nhận không phản hồi. Tiền đã được hoàn lại.");
                        receiver.sendMessage("§c⏳ Đã hết thời gian xác nhận chuyển tiền.");
                        EffectManager.showTradeLeftover(sendPlayer);
                        EffectManager.showTradeLeftover(receiver);

                    }
                }, 20L * 60); // 60 giây
            });
        });

        return true;
    }

    public static Map<UUID, PendingPayRequest> getPendingPayments() {
        return pendingPayments;
    }
}

