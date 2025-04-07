package dev.ktoxz.commands;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;

public class Constant {
	private static Map<String, Object> GLOBAL = new HashMap<>();
	
	public static final Object get(String key) {
		if(GLOBAL.containsKey(key)) return GLOBAL.get(key);
		return null;
	}
	
	public static final void set(String key, Object obj) {
		GLOBAL.put(key, obj); 
	}
	
	public class CONSTANT {
		public static final String CENTRAL_CHEST_POS = "CENTRAL_CHEST_POS";
	}
}

