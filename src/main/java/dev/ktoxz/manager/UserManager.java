package dev.ktoxz.manager;

import java.util.List;
import java.util.UUID;

import org.bson.Document;
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
			return 0;
		}
	}
	
	public static int insertBalance(Player player, double value) {
		try {
			MongoFind finder = new MongoFind("minecraft", "user");
			Document playerFind = finder.One(new Document("playerId", player.getUniqueId().toString()), null);
			
			System.out.println("playerFind = " + playerFind);

			if (playerFind == null || playerFind.isEmpty()) {
				System.out.println("→ User not found → inserting user + balance");
				insertUser(player, value);
				return 2;
			}

			MongoUpdate updater = new MongoUpdate("minecraft", "user");
			updater.Update(
					new Document("playerId", player.getUniqueId().toString()), 
					new Document("$inc", new Document("balance", value))
				);
			
			return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return -2;
		}
	}
	
	public static Document getPlayer(Player player) {
		MongoFind finder = new MongoFind("minecraft", "user");
		Document fUser = finder.One(new Document("playerId", player.getUniqueId().toString()), null);
		return fUser;
	}
	
	public static void showBalance(Player player) {
	    MongoFind finder = new MongoFind("minecraft", "user");
	    Document playerFind = finder.One(new Document("playerId", player.getUniqueId().toString()), null);

	    if (playerFind == null) {
	        player.sendMessage("§cBạn chưa có tài khoản để hiện thị số dư, qua nhà Ktoxz đi.");
	        return;
	    }

	    double balance = playerFind.getDouble("balance");
	    player.sendMessage("§aTiền hiện tại của bạn: §e" + String.format("%.3f", balance));
	}

}
