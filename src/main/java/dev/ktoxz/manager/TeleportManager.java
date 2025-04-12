package dev.ktoxz.manager;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bukkit.Bukkit;
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
}
