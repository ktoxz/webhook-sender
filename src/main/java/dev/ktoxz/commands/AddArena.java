package dev.ktoxz.commands;

import com.mongodb.client.MongoCollection;
import dev.ktoxz.db.Mongo;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion; // Import ProtectedRegion
import com.sk89q.worldguard.protection.regions.RegionContainer;


public class AddArena implements CommandExecutor {

    private final Plugin plugin;

    public AddArena(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cSử dụng: /addArena <tên_region>");
            return true;
        }

        String regionName = args[0];

        // Lấy WorldGuard region
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regionManager == null) {
                player.sendMessage("§cKhông tìm thấy RegionManager cho thế giới này.");
                return true;
            }

            // --- Đã sửa lỗi ở đây ---
            ProtectedRegion region = regionManager.getRegion(regionName); // Lấy region
            if (region == null) { // Kiểm tra nếu region không tồn tại
                player.sendMessage("§cKhông tìm thấy WorldGuard region với tên: " + regionName + ". Vui lòng tạo region đó trước.");
                return true;
            }
            // --- Hết sửa lỗi ---

        } catch (NoClassDefFoundError e) {
            player.sendMessage("§cLỗi: WorldGuard không được tìm thấy. Đảm bảo WorldGuard đã được cài đặt và tải đúng.");
            return true;
        }


        // Lưu vào MongoDB, chỉ với tên region làm _id
        // Đảm bảo Mongo.getInstance().getArenas() đã được định nghĩa và trả về MongoCollection<Document> cho collection "arena"
        MongoCollection<Document> arenas = Mongo.getInstance().getArenas();
        if (arenas == null) {
            player.sendMessage("§cLỗi: Không thể truy cập database để lưu arena. Vui lòng kiểm tra console.");
            plugin.getLogger().severe("Could not get 'arena' collection from MongoDB.");
            return true;
        }

        Document arenaDoc = new Document("_id", regionName)
                .append("spots", new java.util.ArrayList<>()); // Vẫn giữ spots để thêm điểm spawn

        try {
            arenas.insertOne(arenaDoc);
            player.sendMessage("§aArena '" + regionName + "' đã được thêm vào cơ sở dữ liệu (liên kết với WorldGuard region).");
        } catch (Exception e) {
            player.sendMessage("§cLỗi khi lưu arena: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}