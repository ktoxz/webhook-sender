package dev.ktoxz.pvp;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpSessionListener implements Listener {

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
	    Player player = event.getPlayer();

	    if (!PvpSessionManager.isInSession(player)) return;
	    PvpSession session = PvpSessionManager.getSession(player);

	    if (session == null) return;

	    // 👉 Chỉ chặn move nếu đang trong countdownPhase
	    if (session.isCountdownPhase()) {
	        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
	            event.setCancelled(true);
	        }
	    }
	}



	@EventHandler
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	    if (!(sender instanceof Player player)) {
	        sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này.");
	        return true;
	    }

	    if (!PvpSessionManager.hasActiveSession()) {
	        player.sendMessage("§cKhông có phòng PvP nào để hủy.");
	        return true;
	    }

	    if (!PvpSessionManager.isOwner(player)) {
	        player.sendMessage("§cChỉ chủ phòng mới có thể hủy phòng PvP.");
	        return true;
	    }

	    if (PvpSessionManager.getActiveSession().isStarted() || PvpSessionManager.getActiveSession().isCountdownPhase()) {
	        player.sendMessage("§cKhông thể xài lệnh khi trận đấu đã bắt đầu!");
	        return true;
	    }

	    PvpSessionManager.getActiveSession().broadcast("§cPhòng PvP của " + player.getName() + " đã bị hủy.");
	    PvpSessionManager.closeSession();
	    return true;
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
	    Player player = event.getPlayer();

	    if (!PvpSessionManager.isInSession(player)) return;
	    PvpSession session = PvpSessionManager.getSession(player);

	    if (session == null) return;

	    // Nếu session đã bắt đầu (trận đã start)
	    if (session.isStarted() || session.isCountdownPhase()) {
	        event.setCancelled(true);
	        player.sendMessage("§cKhông thể sử dụng lệnh khi trận PvP đang diễn ra!");
	    }
	}

	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
	    Player player = event.getEntity();

	    if (!PvpSessionManager.isInSession(player)) return;
	    
	    // Remove người chết khỏi session
	    PvpSessionManager.removePlayer(player);

	    // Check còn 1 người sống sót
	    checkForWin();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();
	    
	    if (!PvpSessionManager.isInSession(player)) return;
	    
	    // Remove người quit khỏi session
	    PvpSessionManager.removePlayer(player);
	    
	    // Check còn 1 người sống sót
	    checkForWin();
	}

	private void checkForWin() {
	    if (!PvpSessionManager.hasActiveSession()) return;

	    PvpSession session = PvpSessionManager.getActiveSession();
	    for(Player p : session.getPlayers()) {
	    	System.out.println(p.getName());
	    }
	    
	    if(!session.isStarted()) return;
	    
	    if (session.getPlayers().size() == 1 && session.isStarted()) {
	        session.handleVictory();
	        PvpSessionManager.closeSession();
	    }
	}
	
	@EventHandler
	public void onChestClick(InventoryClickEvent event) {
	    if (!(event.getWhoClicked() instanceof Player player)) return;
	    if (event.getClickedInventory() == null) return;
	    if (event.getCurrentItem() == null) return;

	    if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {

	        // Nếu click vào chest để lấy đồ ra
	        if (event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
	            
	            // Đếm lại toàn bộ inventory player nếu thêm món sắp lấy
	            int count = countInventoryAfterTaking(player, event.getCurrentItem());

	            if (count > 10) {
	                event.setCancelled(true);
	                player.sendMessage("§c❌ Bạn chỉ được giữ tối đa 10 vật phẩm từ rương!");
	            }
	        }
	    }
	}
	
	private int countInventoryAfterTaking(Player player, ItemStack newItem) {
	    int count = 0;

	    for (ItemStack item : player.getInventory().getContents()) {
	        if (item != null && item.getType() != Material.AIR) {
	            count++;
	        }
	    }

	    if (newItem != null && newItem.getType() != Material.AIR) {
	        count++;
	    }

	    return count;
	}



}
