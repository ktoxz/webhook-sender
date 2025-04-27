package dev.ktoxz.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.mongodb.client.FindIterable;

import dev.ktoxz.db.MongoFind;
import dev.ktoxz.db.MongoUpdate;

public class TeleportManager {
	
	static List<Document> result = new ArrayList<>();
	
	public static boolean isEnough(Player player) {
		Document fPlayer = UserManager.getPlayer(player);
		if(fPlayer == null) return false;
		Document event = findService("tele_to_player");
		return fPlayer.getDouble("balance") >= event.getDouble("price");
	}
	
	public static boolean isEnoughSpot(Player player) {
		Document fPlayer = UserManager.getPlayer(player);
		if(fPlayer == null) return false;
		Document event = findService("teleport_to_spot");
		return fPlayer.getDouble("balance") >= event.getDouble("price");
	}
	
	public static void useTp(Player player, String nameEvent) {
	    Bukkit.getScheduler().runTaskAsynchronously(
	        Bukkit.getPluginManager().getPlugin("KtoxzWebhook"),
	        () -> {
	            MongoUpdate updater = new MongoUpdate("minecraft", "user");
	            Document event = findService(nameEvent);
	            updater.Update(
	                new Document("playerId", player.getUniqueId().toString()),
	                new Document("$inc", new Document("balance", -event.getDouble("price")))
	            );
	        }
	    );
	}


	
	public static List<Document> getTpSpots(Boolean refresh) {
		if(!refresh) return result;
		MongoFind finder = new MongoFind("minecraft", "tpSpot");
		FindIterable<Document> iterable = finder.Many(null, null);
		result.clear();
		for (Document doc : iterable) {
		    result.add(doc);
		}
		return result;
	}
	
	public static Document findService(String name) {
		MongoFind finder = new MongoFind("minecraft", "service");
		return finder.One(new Document("_id", name), null);
	}
	
	//ArenaStuff
	
	
	private static final Random random = new Random();

    public static Document getRandomArena() {
        MongoFind finder = new MongoFind("minecraft", "arena");
        List<Document> arenas = finder.Many(null, null).into(new ArrayList<>());
        if (arenas.isEmpty()) return null;
        return arenas.get(random.nextInt(arenas.size()));
    }

    public static void teleportPlayerToRandomSpotInArena(Player player, Document arena) {
        if (arena == null) {
            player.sendMessage("§cKhông tìm thấy arena hợp lệ!");
            return;
        }

        List<Document> spots = arena.getList("spots", Document.class);
        if (spots == null || spots.isEmpty()) {
            player.sendMessage("§cArena '" + arena.getString("_id") + "' chưa có spot.");
            return;
        }

        Document spot = spots.get(random.nextInt(spots.size()));
        int x = spot.getInteger("x");
        int y = spot.getInteger("y");
        int z = spot.getInteger("z");

        World world = Bukkit.getWorld("world"); // chỉnh tên world nếu bạn dùng nhiều thế giới
        if (world != null) {
            player.teleport(new Location(world, x + 0.5, y, z + 0.5));
        } else {
            player.sendMessage("§cWorld không tồn tại.");
        }
    }

}
