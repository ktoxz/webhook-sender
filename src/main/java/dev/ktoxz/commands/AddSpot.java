package dev.ktoxz.commands;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mongodb.MongoWriteException;

import dev.ktoxz.db.MongoInsert;
import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;

public class AddSpot implements CommandExecutor {
	
    private final KtoxzWebhook plugin;

    public AddSpot(KtoxzWebhook plugin) {
        this.plugin = plugin;
        TeleportManager.getTpSpots(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cDùng đúng: /addspot <tên>");
            return true;
        }

        String spotName = args[0].toLowerCase();
        Location loc = player.getLocation();

        Document doc = new Document("name", spotName)
                .append("x", (int)loc.getX())
                .append("y", (int)loc.getY())
                .append("z", (int)loc.getZ());

        MongoInsert inserter = new MongoInsert("minecraft", "tpSpot");
    	try {
			inserter.One(doc);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	 player.sendMessage("§aĐã lưu địa điểm §e" + spotName + " §avới tọa độ: §7(" +
                 (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + ")");
        
        

       
        
        TeleportManager.getTpSpots(true);
        return true;
    }
}
