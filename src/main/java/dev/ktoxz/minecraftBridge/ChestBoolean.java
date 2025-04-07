package dev.ktoxz.minecraftBridge;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

import dev.ktoxz.commands.Constant;

public class ChestBoolean {
	
	public static boolean isCentralChest(InventoryOpenEvent event) {
		return event.getInventory().getType() == InventoryType.CHEST &&
    		    event.getInventory().getLocation() ==  Constant.get(Constant.CONSTANT.CENTRAL_CHEST_POS);
	}
}
