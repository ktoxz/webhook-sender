package dev.ktoxz.manager;

import java.util.List;

import org.bson.Document;
import org.bukkit.entity.Player;

import dev.ktoxz.db.MongoFind;
import dev.ktoxz.db.MongoInsert;

public class TransactionManager {

	public static int insertTransaction(List itemList, Player player) {
		try {
			if (!itemList.isEmpty()) {
		        // Tạo id tăng dần (bằng count + 1)
		        MongoFind finder = new MongoFind("minecraft", "transactions");
		        long count = finder.getCol().countDocuments();
		        int id = (int) count + 1;

		        Document transaction = new Document()
		            .append("id", id)
		            .append("user", player.getName())
		            .append("listItem", itemList);

		        MongoInsert inserter = new MongoInsert("minecraft", "transactions");
		        inserter.One(transaction);

		        return 1;
		    } else return -1;
		} catch (Exception e) {
			return -2;
		}
	    
	    
	    
	    
	}
	
}
