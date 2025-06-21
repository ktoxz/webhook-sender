package dev.ktoxz.listener;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3; // Thêm import này
import dev.ktoxz.main.KtoxzWebhook; // Import plugin chính để lấy WorldGuardPlugin
import dev.ktoxz.pvp.PvpSession;
import dev.ktoxz.pvp.PvpSessionManager;

public class PvpSessionListener implements Listener {

    private final KtoxzWebhook plugin; // Thêm biến plugin

    public PvpSessionListener(KtoxzWebhook plugin) { // Thêm constructor
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!PvpSessionManager.isInSession(player)) return;
        PvpSession session = PvpSessionManager.getSession(player);

        if (session == null) return;

        // 👉 Chỉ chặn move nếu đang trong countdownPhase hoặc đã bắt đầu trận đấu
        if (session.isCountdownPhase()) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        } else if (session.isStarted()) {
            // Kiểm tra WorldGuard region khi trận đấu đã bắt đầu
            Location to = event.getTo();
            if (to == null) return; // Đảm bảo to location không null

            String arenaRegionName = session.getArenaRegionName();
            if (arenaRegionName == null || plugin.getWorldGuardPlugin() == null) {
                // Log lỗi hoặc thông báo nếu không có region name hoặc WorldGuard không được tìm thấy
                plugin.getLogger().warning("Không tìm thấy tên region hoặc WorldGuard! Không thể kiểm tra vùng PvP.");
                return;
            }

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(to.getWorld()));

            if (regionManager == null) {
                plugin.getLogger().warning("Không tìm thấy RegionManager cho world " + to.getWorld().getName());
                return;
            }

            ProtectedRegion arenaRegion = regionManager.getRegion(arenaRegionName);

            if (arenaRegion == null) {
                plugin.getLogger().warning("Không tìm thấy WorldGuard region với ID: " + arenaRegionName);
                return;
            }

            // Kiểm tra nếu người chơi di chuyển ra ngoài region
            // --- Đã sửa lỗi ở đây ---
            // Tạo một BlockVector3 từ tọa độ khối của Location
            BlockVector3 toBlockVector = BlockVector3.at(to.getBlockX(), to.getBlockY(), to.getBlockZ());
            
            if (!PvpSessionManager.isInSession(player) && arenaRegion.contains(toBlockVector)) {
                event.setCancelled(true);
                player.sendMessage("§cBạn không được phép vào khu vực đấu trường PvP!");
                player.teleport(event.getFrom());
                return;
            }
            
            if (!arenaRegion.contains(toBlockVector)) {
            // --- Hết sửa lỗi ---
                event.setCancelled(true);
                player.sendMessage("§cBạn không thể ra khỏi đấu trường PvP!");
                // Teleport người chơi về vị trí cũ hoặc vào giữa arena
                player.teleport(event.getFrom());
            }
        }
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location dropLoc = player.getLocation();

        // Chặn người trong trận không được vứt đồ trong thời gian đếm ngược hoặc đang PvP
        if (PvpSessionManager.isInSession(player)) {
            PvpSession session = PvpSessionManager.getSession(player);
            if (session != null && (session.isStarted() || session.isCountdownPhase())) {
                event.setCancelled(true);
                player.sendMessage("§cKhông được vứt đồ trong đấu trường PvP!");
                return;
            }
        }

        // Nếu có session đang hoạt động, chặn vứt đồ khi đứng ngoài đấu trường
        if (PvpSessionManager.hasActiveSession()) {
            PvpSession session = PvpSessionManager.getActiveSession();
            String regionName = session.getArenaRegionName();

            if (regionName != null) {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionManager regionManager = container.get(BukkitAdapter.adapt(dropLoc.getWorld()));

                if (regionManager != null) {
                    ProtectedRegion region = regionManager.getRegion(regionName);
                    if (region != null) {
                        BlockVector3 vec = BlockVector3.at(dropLoc.getBlockX(), dropLoc.getBlockY(), dropLoc.getBlockZ());

                        // Nếu người chơi KHÔNG thuộc vùng đấu → huỷ
                        if (!region.contains(vec)) {
                            event.setCancelled(true);
                            player.sendMessage("§cBạn không thể vứt đồ ra ngoài khu vực đấu trường!");
                        }
                    }
                }
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

	    PvpSession session = PvpSessionManager.getSession(player); // Lấy session trước khi remove
	    if (session == null) return; // Đảm bảo session không null

	    boolean isOwner = PvpSessionManager.isOwner(player); // Kiểm tra chủ phòng
	    boolean sessionStarted = session.isStarted(); // Kiểm tra trạng thái session

	    // Xóa người chơi khỏi session (được gọi cho cả chủ phòng và người chơi bình thường)
	    PvpSessionManager.removePlayer(player);
	    
	    if (isOwner) {
	        if (!sessionStarted) { // Chủ phòng thoát khi session CHƯA BẮT ĐẦU
	            session.broadcast("§cChủ phòng PvP (" + player.getName() + ") đã thoát, phòng đã bị hủy.");
	            PvpSessionManager.closeSession(); // Hủy toàn bộ session
	            return; // Đã xử lý, thoát
	        } else { // Chủ phòng thoát khi session ĐÃ BẮT ĐẦU (trong trận đấu)
	            // Xử lý như một người chơi bình thường chết (sẽ được checkForWin xử lý)
	            session.broadcast("§cChủ phòng PvP (" + player.getName() + ") đã thoát khỏi trận đấu.");
	        }
	    }

	    // Nếu không phải chủ phòng, hoặc chủ phòng thoát khi trận đấu đang diễn ra,
	    // thì kiểm tra điều kiện chiến thắng
	    checkForWin();
	}

	private void checkForWin() {
	    
		if(!PvpSessionManager.hasActiveSession()) return;

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
		PvpSession session = PvpSessionManager.getActiveSession();
		if(!PvpSessionManager.hasActiveSession()) return;
		if(!session.getPlayers().contains(event.getWhoClicked())) return;
	    if (!(event.getWhoClicked() instanceof Player player)) return;
	    if (event.getClickedInventory() == null) return;
	    if (event.getCurrentItem() == null) return;

	    if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {

	        if (event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {

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