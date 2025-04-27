package dev.ktoxz.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ktoxz.db.MongoUpdate;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class LinkDiscord implements CommandExecutor {

    private final JavaPlugin plugin;

    public LinkDiscord(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dùng cho người chơi.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cDùng đúng: /link <discordId>");
            return true;
        }

        String discordId = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URI uri = URI.create("http://localhost:3000/api/resolve-discord");
                HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JsonObject payload = new JsonObject();
                payload.addProperty("discordId", discordId); // ✅ CHỈNH Ở ĐÂY

                try (OutputStream os = con.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    JsonObject response = JsonParser.parseReader(br).getAsJsonObject();

                    if (response.has("valid") && response.get("valid").getAsBoolean()) {
                        // Lưu vào Mongo nếu hợp lệ
                        new MongoUpdate("minecraft", "user").Update(
                                new Document("playerId", player.getUniqueId().toString()),
                                new Document("$set", new Document("discordId", discordId))
                        );

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§a✔ Đã liên kết thành công với Discord ID: " + discordId);
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage("§c❌ ID không hợp lệ hoặc không tìm thấy người dùng Discord.");
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c❌ Lỗi khi kết nối bot Discord.");
                });
            }
        });

        return true;
    }
}
