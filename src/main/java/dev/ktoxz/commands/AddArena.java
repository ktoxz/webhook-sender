package dev.ktoxz.commands;

import dev.ktoxz.db.MongoInsert;
import dev.ktoxz.main.KtoxzWebhook;

import org.bson.Document;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AddArena implements CommandExecutor {

    private final KtoxzWebhook plugin;

    public AddArena(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }
	
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        if (args.length != 3) {
            player.sendMessage("§cDùng đúng: /addarena <loc1> <loc2> <name>");
            return true;
        }

        // Lấy tọa độ 2 điểm
        String[] loc1 = args[0].split(",");
        String[] loc2 = args[1].split(",");
        String arenaName = args[2].toLowerCase();

        if (loc1.length != 3 || loc2.length != 3) {
            player.sendMessage("§cTọa độ không hợp lệ.");
            return true;
        }

        Document arenaDoc = new Document("_id", arenaName)
                .append("corner1", new Document("x", Integer.parseInt(loc1[0])).append("y", Integer.parseInt(loc1[1])).append("z", Integer.parseInt(loc1[2])))
                .append("corner2", new Document("x", Integer.parseInt(loc2[0])).append("y", Integer.parseInt(loc2[1])).append("z", Integer.parseInt(loc2[2])))
                .append("spots", new java.util.ArrayList<>());

        try {
            new MongoInsert("minecraft", "arena").One(arenaDoc);
            player.sendMessage("§a✔ Đã thêm arena " + arenaName);
        } catch (Exception e) {
            player.sendMessage("§c❌ Lỗi khi lưu arena.");
            e.printStackTrace();
        }

        return true;
    }
}
