package dev.ktoxz.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.UserManager;

public class GiveBalance implements CommandExecutor {
	
    private final KtoxzWebhook plugin;

    public GiveBalance(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ktoxz.admin")) {
            sender.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("§cDùng đúng: /givebalance <tên người chơi> <số tiền>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cNgười chơi không tồn tại hoặc không online.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage("§cSố tiền phải lớn hơn 0.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cSố tiền không hợp lệ.");
            return true;
        }

        int result = UserManager.insertBalance(target, amount);
        switch (result) {
            case 1:
                sender.sendMessage("§aĐã cộng §e" + String.format("%.3f", amount) + "§a vào tài khoản của §e" + target.getName());
                target.sendMessage("§aBạn vừa nhận được §e" + String.format("%.3f", amount) + "§a từ §b" + sender.getName() + "§a!");
                break;
            case 2:
                sender.sendMessage("§aĐã tạo tài khoản mới và cộng §e" + String.format("%.3f", amount) + "§a cho §e" + target.getName());
                target.sendMessage("§aBạn vừa nhận được §e" + String.format("%.3f", amount) + "§a từ §b" + sender.getName() + "§a!");
                break;
            default:
                sender.sendMessage("§cĐã có lỗi khi cộng tiền.");
        }

        return true;
    }
}
