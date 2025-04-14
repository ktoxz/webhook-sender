package dev.ktoxz.manager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.ktoxz.db.MongoFind;
import dev.ktoxz.db.MongoInsert;
import dev.ktoxz.db.MongoUpdate;

public class UserManager {

    public static int insertUser(Player player, double value) {
        try {
            UUID uuid = player.getUniqueId();
            Document transaction = new Document()
                .append("playerId", uuid.toString())
                .append("name", player.getName())
                .append("balance", value);
            MongoInsert inserter = new MongoInsert("minecraft", "user");
            inserter.One(transaction);
            return 1;
        } catch (Exception e) {
            return -1;
        }
    }

    // Tăng balance của player (async) và trả về mã trạng thái
    public static int insertBalance(Player player, double amount) {
        try {
            UUID uuid = player.getUniqueId();
            String name = player.getName();

            MongoFind finder = new MongoFind("minecraft", "user");
            Document playerFind = finder.One(new Document("playerId", uuid.toString()), null);

            if (playerFind == null) {
                // Người chơi chưa có trong DB → thêm mới
                Document insertDoc = new Document()
                        .append("playerId", uuid.toString())
                        .append("name", name)
                        .append("balance", amount);

                new MongoInsert("minecraft", "user").One(insertDoc);
                Bukkit.getLogger().info("📦 Đã tạo mới user " + name + " với " + amount + " coin.");
            } else {
                // Người chơi đã tồn tại → cộng thêm tiền
            	double currentBalance = playerFind.get("balance", Number.class).doubleValue();
                double newBalance = currentBalance + amount;

                Document filters = new Document("playerId", uuid.toString());
                Document updates = new Document("$set", new Document("balance", newBalance));

                new MongoUpdate("minecraft", "user").Update(filters, updates);
                Bukkit.getLogger().info("💰 Cập nhật tiền cho " + name + ": " + currentBalance + " ➜ " + newBalance);
            }

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static Document getPlayer(Player player) {
        MongoFind finder = new MongoFind("minecraft", "user");
        Document fUser = finder.One(new Document("playerId", player.getUniqueId().toString()), null);
        return fUser;
    }

    public static void showBalance(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("KtoxzWebhook"),
            () -> {
                MongoFind finder = new MongoFind("minecraft", "user");
                Document playerFind = finder.One(new Document("playerId", player.getUniqueId().toString()), null);

                if (playerFind == null) {
                    Bukkit.getScheduler().runTask(
                        Bukkit.getPluginManager().getPlugin("KtoxzWebhook"),
                        () -> player.sendMessage("§cBạn chưa có tài khoản để hiện thị số dư, qua nhà Ktoxz đi.")
                    );
                    return;
                }

                double balance = playerFind.getDouble("balance");
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("KtoxzWebhook"),
                    () -> player.sendMessage("§aTiền hiện tại của bạn: §e" + String.format("%.3f", balance))
                );
            }
        );
    }
    
    public static void getBalanceAsync(Player player, Consumer<Double> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("KtoxzWebhook"), () -> {
            MongoFind finder = new MongoFind("minecraft", "user");
            Document playerFind = finder.One(new Document("playerId", player.getUniqueId().toString()), null);

            if (playerFind == null) {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("KtoxzWebhook"), () -> {
                    player.sendMessage("§cBạn chưa có tài khoản để hiện thị số dư, qua nhà Ktoxz đi.");
                });
                return;
            }

            double balance = playerFind.getDouble("balance");

            // Gọi lại callback ở main thread
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("KtoxzWebhook"), () -> {
                callback.accept(balance);
            });
        });
    }

}
