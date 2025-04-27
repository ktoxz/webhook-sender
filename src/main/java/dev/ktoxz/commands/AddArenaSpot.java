package dev.ktoxz.commands;

import dev.ktoxz.db.MongoUpdate;
import dev.ktoxz.main.KtoxzWebhook;

import org.bson.Document;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AddArenaSpot implements CommandExecutor {

    private final KtoxzWebhook plugin;

    public AddArenaSpot(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }
	

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cDùng đúng: /addarenaspot <arena_name>");
            return true;
        }

        String arenaName = args[0].toLowerCase();

        Document spot = new Document("x", (int)player.getLocation().getX())
                .append("y", (int)player.getLocation().getY())
                .append("z", (int)player.getLocation().getZ());

        new MongoUpdate("minecraft", "arena").Update(
            new Document("_id", arenaName),
            new Document("$push", new Document("spots", spot))
        );

        player.sendMessage("§a✔ Đã thêm spot vào arena " + arenaName);
        return true;
    }
}
