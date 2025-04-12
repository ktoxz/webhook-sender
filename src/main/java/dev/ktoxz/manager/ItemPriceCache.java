package dev.ktoxz.manager;

import org.bson.Document;
import dev.ktoxz.db.MongoFind;

import java.util.HashMap;
import java.util.Map;

public class ItemPriceCache {
    private static final Map<String, Double> itemPriceMap = new HashMap<>();

    public static void load() {
        MongoFind finder = new MongoFind("minecraft", "itemTrade");
        for (Document doc : finder.Many(null, null)) {
            itemPriceMap.put(doc.getString("_id"), doc.getDouble("price"));
        }
    }

    public static Double getPrice(String itemId) {
        return itemPriceMap.get(itemId);
    }

    public static boolean contains(String itemId) {
        return itemPriceMap.containsKey(itemId);
    }
}
