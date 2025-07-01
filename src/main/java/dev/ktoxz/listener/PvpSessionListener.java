package dev.ktoxz.listener;

import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World; 

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;

import dev.ktoxz.main.KtoxzWebhook;
import dev.ktoxz.pvp.PvpSession;
import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.PvpSession.PvpSessionState;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class PvpSessionListener implements Listener {

    private final KtoxzWebhook plugin;

    public PvpSessionListener(KtoxzWebhook plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        PvpSession session = PvpSessionManager.getSession(player);

        // Trường hợp 1: Người chơi KHÔNG trong bất kỳ session nào
        if (session == null) {
            // Nếu có session PvP đang hoạt động
            if (PvpSessionManager.hasActiveSession()) {
                PvpSession activeSession = PvpSessionManager.getActiveSession();
                String arenaRegionName = activeSession.getArenaRegionName();
                if (arenaRegionName != null) {
                    RegionManager regionManager = getRegionManager(to.getWorld());
                    if (regionManager == null) return;

                    ProtectedRegion arenaRegion = regionManager.getRegion(arenaRegionName);
                    if (arenaRegion == null) return;

                    BlockVector3 toBlock = BlockVector3.at(to.getBlockX(), to.getBlockY(), to.getBlockZ());
                    BlockVector3 fromBlock = BlockVector3.at(from.getBlockX(), from.getBlockY(), from.getBlockZ());

                    // Nếu người chơi đang cố gắng đi VÀO region của arena mà không phải là thành viên session
                    if (!arenaRegion.contains(fromBlock) && arenaRegion.contains(toBlock)) {
                        event.setCancelled(true);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cBạn không thể vào đấu trường PvP khi không tham gia phiên!"));
                    }
                }
            }
            return;
        }

        // Trường hợp 2: Người chơi ĐANG trong một session PvP
        // Nếu session đang trong giai đoạn đếm ngược (COUNTDOWN), không cho di chuyển (người chơi phải đứng yên)
        if (session.isCountdownPhase()) {
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                event.setCancelled(true);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cVui lòng đứng yên trong thời gian đếm ngược!"));
            }
            return;
        }

        // Nếu session đã BẮT ĐẦU (STARTED), không cho phép người chơi rời khỏi WorldGuard region của arena
        if (session.isStarted()) {
            String arenaRegionName = session.getArenaRegionName();
            if (arenaRegionName == null) {
                plugin.getLogger().warning("PvpSession is started but arenaRegionName is null for player " + player.getName());
                return;
            }

            RegionManager regionManager = getRegionManager(to.getWorld());
            if (regionManager == null) return;

            ProtectedRegion arenaRegion = regionManager.getRegion(arenaRegionName);
            if (arenaRegion == null) {
                plugin.getLogger().warning("Không tìm thấy region với tên: " + arenaRegionName + " cho session của " + player.getName());
                return;
            }
            
            BlockVector3 toBlock = BlockVector3.at(to.getBlockX(), to.getBlockY(), to.getBlockZ());
            BlockVector3 fromBlock = BlockVector3.at(from.getBlockX(), from.getBlockY(), from.getBlockZ());

            boolean isMovingOutOfRegion = arenaRegion.contains(fromBlock) && !arenaRegion.contains(toBlock);

            if (isMovingOutOfRegion) {
                event.setCancelled(true);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cBạn không thể rời khỏi đấu trường PvP!"));
            }
        }
    }
    
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!PvpSessionManager.isInSession(player)) return;
        PvpSession session = PvpSessionManager.getSession(player);

        if (session == null) return;

        if (session.isStarted() || session.isCountdownPhase()) {
            event.setCancelled(true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cKhông thể sử dụng lệnh khi trận PvP đang diễn ra!")); // Chuyển sang hotbar
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!PvpSessionManager.isInSession(player)) return;
        
        PvpSessionManager.removePlayer(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (!PvpSessionManager.isInSession(player)) return;

        PvpSession session = PvpSessionManager.getSession(player);
        if (session == null) return;

        boolean isOwner = PvpSessionManager.isOwner(player);
        
        PvpSessionManager.removePlayer(player);
        
        
        // Broadcast thông báo cho tất cả người chơi trong phiên (chứ không riêng người chơi vừa thoát)
        // Thông báo này sẽ được gửi qua broadcast() trong PvpSession, vốn là sendMessage()
        // Nếu muốn các thông báo này cũng hiển thị trên hotbar của TẤT CẢ người chơi,
        // bạn sẽ cần sửa phương thức broadcast() trong PvpSession để sử dụng action bar.
        // Hiện tại, tôi giữ nguyên cho các thông báo này đi qua broadcast() của session.
        if (isOwner) {
            if (session.getCurrentState() == PvpSessionState.WAITING) {
                session.broadcast("§cChủ phòng PvP (" + player.getName() + ") đã thoát, phòng đã bị hủy."); 
            } else { 
                session.broadcast("§cChủ phòng PvP (" + player.getName() + ") đã thoát khỏi trận đấu.");
            }
        }
    }
    
    @EventHandler
    public void onChestClick(InventoryClickEvent event) {
        PvpSession session = PvpSessionManager.getActiveSession();

        if (session == null || !session.getPlayers().contains(event.getWhoClicked()) || !session.isCountdownPhase()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;

        if (event.getView().getTopInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
            if (event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
                int currentItemCount = countValidItemsInPlayerInventory(player);

                if (currentItemCount >= 10) {
                    event.setCancelled(true);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c❌ Bạn chỉ được giữ tối đa 10 vật phẩm từ rương khởi đầu!")); // Chuyển sang hotbar
                }
            }
        }
    }
    
    private int countValidItemsInPlayerInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    private RegionManager getRegionManager(World world) {
        if (plugin.getWorldGuardPlugin() == null) {
            plugin.getLogger().warning("WorldGuard plugin is not available. Region protection will not work.");
            return null;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        PvpSession session = PvpSessionManager.getSession(player);
        if (session == null || !session.isCountdownPhase()) return;

        String arenaRegionName = session.getArenaRegionName();
        if (arenaRegionName == null) return;

        RegionManager regionManager = getRegionManager(event.getTo().getWorld());
        if (regionManager == null) return;

        ProtectedRegion arenaRegion = regionManager.getRegion(arenaRegionName);
        if (arenaRegion == null) return;

        BlockVector3 destination = BlockVector3.at(
            event.getTo().getBlockX(),
            event.getTo().getBlockY(),
            event.getTo().getBlockZ()
        );

        if (!arenaRegion.contains(destination)) {
            event.setCancelled(true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§cKhông thể dịch chuyển ra ngoài đấu trường PvP!"));
            return;
        }

        // ⛔ Ngăn cầm item trên con trỏ khi PvP đang diễn ra
        if (session.isCountdownPhase() || session.isStarted()) {
            ItemStack cursorItem = player.getItemOnCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.setItemOnCursor(null);
                player.getInventory().addItem(cursorItem);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§cBạn không thể giữ vật phẩm trên tay khi đang PvP! Vật phẩm đã được trả lại kho."));
            }
        }
        player.closeInventory();
    }



}