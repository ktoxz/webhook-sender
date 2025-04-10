package dev.ktoxz.manager;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class TransactionManager {
	
	public static Map<String, String> pendingRequests = new HashMap<>();

	public static int insertTransaction(List itemList, Player player, double totalPrice) {
		if (itemList == null || itemList.isEmpty()) return -1;

		try {
			UUID uuid = player.getUniqueId();
			String name = player.getName();
			Date now = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			// Chuẩn bị nội dung ghi log
			StringBuilder log = new StringBuilder();
			log.append("Time: ").append(sdf.format(now)).append("\n");
			log.append("Player: ").append(uuid.toString()).append("\n");
			log.append("Name: ").append(name).append("\n");
			log.append("Total Price: ").append(totalPrice).append("\n");
			log.append("Items: ").append(itemList.toString()).append("\n");
			log.append("-----------\n");

			// Ghi vào file
			FileWriter writer = new FileWriter("transaction.log", true); // true = append
			writer.write(log.toString());
			writer.close();

			// Optional: xử lý cộng tiền
			int resCode = UserManager.insertBalance(player, totalPrice);
			if (resCode == 1) return 2;

			return 1;

		} catch (IOException e) {
			e.printStackTrace();
			return -2;
		}
	}
}
