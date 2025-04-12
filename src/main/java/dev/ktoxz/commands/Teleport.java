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
import dev.ktoxz.manager.EffectManager;
import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.manager.UserManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Teleport implements CommandExecutor, TabCompleter {

    private final KtoxzWebhook plugin;

    public Teleport(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cDùng đúng: /tp <địa điểm|tên người chơi>");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String arg = args[0].toLowerCase();
            Location spot = getSpotLocation(arg);

            if (spot != null) {
                startCountdown(player, () -> {
                    if (!player.isOnline()) return;
                    player.teleport(spot);
                    player.sendMessage("§aDịch chuyển đến địa điểm: " + arg);
                    TeleportManager.useTp(player, "teleport_to_spot");
                    UserManager.showBalance(player);
                });
            } else {
                teleportToPlayerAsync(player, arg);
            }
        });

        return true;
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

    private void teleportToPlayerAsync(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cNgười chơi không tồn tại hoặc không online.");
            });
            return;
        }

        if (target.getName().equals(player.getName())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cBạn không thể teleport đến chính mình.");
            });
            return;
        }

        if (!TeleportManager.isEnough(player)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cHết tiền roài.");
            });
            return;
        }

        startCountdown(player, () -> {
            if (!player.isOnline() || !target.isOnline()) return;
            player.teleport(target.getLocation());
            player.sendMessage("§aĐã dịch chuyển đến " + target.getName());
            EffectManager.showTeleportComplete(player);
            TeleportManager.useTp(player, "tele_to_player");
            UserManager.showBalance(player);
        });
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
    
    private void startCountdown(Player player, Runnable onFinish) {
        final int[] count = {5};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }

            if (count[0] <= 0) {
                task.cancel();
                onFinish.run();
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§eDịch chuyển sau §c" + count[0] + " §egiây..."));
                count[0]--;
            }
        }, 0L, 20L); // mỗi 20 ticks (1 giây)
    }


   
}
