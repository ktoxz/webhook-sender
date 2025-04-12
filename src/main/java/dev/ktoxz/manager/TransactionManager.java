package dev.ktoxz.manager;

import com.mongodb.client.MongoCollection;
import dev.ktoxz.db.MongoInsert;
import dev.ktoxz.db.MongoFind;
import dev.ktoxz.manager.UserManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class TransactionManager {

    public static void insertTransactionAsync(List<Document> itemList, Player player, Runnable callback) {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("KtoxzWebhook"), () -> {
            if (itemList.isEmpty()) {
                Bukkit.getLogger().warning("❌ Không có vật phẩm hợp lệ để giao dịch.");
                return;
            }

            UUID uuid = player.getUniqueId();
            String name = player.getName();
            Document transaction = new Document()
                    .append("id", System.currentTimeMillis())
                    .append("user", new Document("uuid", uuid.toString()).append("name", name))
                    .append("listItem", itemList);

            try {
                new MongoInsert("minecraft", "transactions").One(transaction);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            int status = insertBalance(player, calculateTotal(itemList));

            if (callback != null) {
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("KtoxzWebhook"), () -> {
                    if (status == 1) {
                        callback.run();
                    } else {
                        player.sendMessage("§c❌ Lỗi khi cộng tiền sau giao dịch.");
                    }
                });
            }
        });
    }

    private static double calculateTotal(List<Document> items) {
        double total = 0;
        for (Document doc : items) {
            Number price = doc.get("price", Number.class);
            int amount = doc.getInteger("quantity", 1);
            total += (price != null ? price.doubleValue() * amount : 0f);
        }
        return total;
    }

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
            	double currentBalance = playerFind.get("balance", Number.class).intValue();
                double newBalance = currentBalance + amount;

                Document filters = new Document("playerId", uuid.toString());
                Document updates = new Document("$set", new Document("balance", newBalance));

                new dev.ktoxz.db.MongoUpdate("minecraft", "user").Update(filters, updates);
                Bukkit.getLogger().info("💰 Cập nhật tiền cho " + name + ": " + currentBalance + " ➜ " + newBalance);
            }

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
