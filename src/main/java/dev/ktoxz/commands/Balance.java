package dev.ktoxz.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.UserManager;

public class Balance implements CommandExecutor {
	
    private final KtoxzWebhook plugin;

    public Balance(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;

        // Gọi hàm hiển thị số dư
        UserManager.showBalance(player);

        return true;
    }
}
