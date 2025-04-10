package dev.ktoxz.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.ktoxz.db.MongoFind;
import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.manager.UserManager;

public class Teleport implements CommandExecutor, TabCompleter {

    private final KtoxzWebhook plugin;

    public Teleport(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cDùng đúng: /tp <địa điểm|tên người chơi>");
            return true;
        }

        String arg = args[0].toLowerCase();

        // Thử tìm theo địa điểm lưu trong Mongo
        
        Location spot = getSpotLocation(arg);
        if(teleportToSpot(player, spot)) {
            player.sendMessage("§aDịch chuyển đến địa điểm: " + arg);
        	return true;
        }
        // Teleport đến người chơi khác
        return teleportToPlayer(player, arg);
    }

    private Location getSpotLocation(String name) {
        List<Document> spots = TeleportManager.getTpSpots(false); // dùng cache
        for (Document doc : spots) {
            if (doc.getString("name").equalsIgnoreCase(name)) {
                World world = Bukkit.getWorld("world");
                if (world == null) return null;
                double x = doc.getInteger("x");
                double y = doc.getInteger("y");
                double z = doc.getInteger("z");
                return new Location(world, x, y, z);
            }
        }
        return null;
    }
    
    private boolean teleportToSpot(Player player,Location spot) {
    	
    	if (spot != null) {
            player.teleport(spot);
        	MongoFind finder = new MongoFind("minecraft", "user");
            TeleportManager.useTp(player, "teleport_to_spot");
            UserManager.showBalance(player);
            return true;
        }
    	return false;
    }

    private boolean teleportToPlayer(Player player, String targetName) {
        if (!TeleportManager.isEnough(player)) {
            player.sendMessage("§cHết tiền roài.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cNgười chơi không tồn tại hoặc không online.");
            return true;
        }

        if (target.getName().equals(player.getName())) {
            player.sendMessage("§cBạn không thể teleport đến chính mình.");
            return true;
        }

        // Thực hiện dịch chuyển & trừ tiền
        player.teleport(target.getLocation());
        TeleportManager.useTp(player, "tele_to_player");
        player.sendMessage("§aĐã dịch chuyển đến " + target.getName());
        UserManager.showBalance(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();

            // Gợi ý các spot từ MongoDB
            List<Document> spots = TeleportManager.getTpSpots(false);
            for (Document doc : spots) {
                suggestions.add(doc.getString("name"));
            }

            // Gợi ý tên người chơi online
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
   
}
