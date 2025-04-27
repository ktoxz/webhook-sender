package dev.ktoxz.pvp;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
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
	    
	    player.sendMessage("Tôi vừa chết và cúc khỏi trận :v");
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
	    
	    if (session.getPlayers().size() == 1) {
	        session.handleVictory();
	        PvpSessionManager.closeSession();
	    }
	}

}
