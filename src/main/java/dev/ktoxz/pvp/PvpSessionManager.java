package dev.ktoxz.pvp;

import org.bukkit.Bukkit; // Thêm import Bukkit
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet; // Sử dụng HashSet để sao chép Set khi lặp
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import dev.ktoxz.pvp.PvpSession.PvpSessionState;

public class PvpSessionManager {

    private static final Map<UUID, PvpSession> playerSessionMap = new HashMap<>();
    private static PvpSession activeSession = null;
    private static Plugin plugin;

    public static void init(Plugin pl) {
        plugin = pl;
    }

    public static boolean hasActiveSession() {
        return activeSession != null;
    }
    
    public static boolean hasStartedSession() {
        return activeSession != null && activeSession.isStarted();
    }

    public static PvpSession getActiveSession() {
        return activeSession;
    }

    public static void createSession(Player owner, boolean isPublic) {
        if (activeSession != null) {
            owner.sendMessage("§cĐã có một phiên PvP đang hoạt động. Vui lòng chờ.");
            return;
        }
        activeSession = new PvpSession(plugin, owner, isPublic);
        registerPlayer(owner, activeSession);
        plugin.getLogger().info("[PvpSessionManager] Phiên PvP mới được tạo bởi " + owner.getName() + (isPublic ? " (Công khai)" : " (Riêng tư)"));
    }
    
    /**
     * Đóng phiên PvP hiện tại và dọn dẹp tất cả tài nguyên.
     * Đảm bảo tất cả các thao tác Bukkit API được thực hiện trên luồng chính.
     */
    public static void closeSession() {
        plugin.getLogger().info("[PvpSessionManager] Đang đóng phiên PvP...");
        
        // Nếu không có phiên hoạt động, thoát
        if (activeSession == null) {
            plugin.getLogger().info("[PvpSessionManager] Không có phiên PvP nào đang hoạt động để đóng.");
            return;
        }

        // Lưu trữ tham chiếu đến phiên đang đóng và đặt activeSession về null ngay lập tức
        // để ngăn chặn việc tương tác không mong muốn với phiên này sau khi bắt đầu quá trình đóng.
        final PvpSession sessionToClose = activeSession;
        activeSession = null; 

        // Luôn chạy các thao tác cleanup liên quan đến Bukkit API trên Main Thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("[PvpSessionManager] Đang thực hiện dọn dẹp Bukkit API trên luồng chính.");

            // 1. Hủy các sự kiện PvP đang chạy và dọn dẹp Entity (ví dụ: SummonEvent.onEndMatch)
            PvpEventManager.clearEvents();

            // 2. Phá hủy tất cả rương được spawn trong phiên
            sessionToClose.destroyChests();

            // 4. Hủy tất cả các tác vụ Bukkit liên quan đến phiên (timeout, countdown, random event)
            // (Đã được chuyển từ PvpSession.clearSessionResources)
            // Cần thêm phương thức cancelAllSessionTasks vào PvpSession
            // Hoặc gọi trực tiếp sessionToClose.cancelSessionTimeout(); // ... other tasks if any ...
            sessionToClose.cancelSessionTimeout(); // Hủy timeout
            // sessionToClose.cancelCountdownTask(); // Nếu có hàm hủy riêng cho countdown
            // sessionToClose.cancelRandomEventTask(); // Nếu có hàm hủy riêng cho random event

            // 5. Hủy đăng ký người chơi khỏi bản đồ quản lý session
            for (Player p : new HashSet<>(sessionToClose.getPlayers())) { // Tạo bản sao để tránh ConcurrentModificationException
                unregisterPlayer(p);
            }
            // Xóa danh sách người chơi được mời (nếu có)
            sessionToClose.getInvitedPlayers().clear();

            plugin.getLogger().info("[PvpSessionManager] Đã hoàn tất dọn dẹp đồng bộ. Bắt đầu dọn dẹp không đồng bộ.");

            // 6. Xóa thành viên khỏi WorldGuard region và lưu (không đồng bộ)
            // Đây là phần duy nhất vẫn cần được chạy không đồng bộ để tránh lag.
            sessionToClose.clearMembersFromActiveRegionAsync()
                .thenRun(() -> {
                    plugin.getLogger().info("[PvpSessionManager] Đã hoàn tất dọn dẹp WorldGuard cho phiên.");
                })
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "[PvpSessionManager] Lỗi khi dọn dẹp WorldGuard region cho phiên: " + ex.getMessage(), ex);
                    return null;
                });
            
            plugin.getLogger().info("[PvpSessionManager] Phiên PvP đã đóng hoàn toàn và tài nguyên được dọn dẹp.");
        });
    }

    public static void registerPlayer(Player player, PvpSession session) {
        playerSessionMap.put(player.getUniqueId(), session);
    }

    public static void unregisterPlayer(Player player) {
        playerSessionMap.remove(player.getUniqueId());
    }

    public static boolean isInSession(Player player) {
        return playerSessionMap.containsKey(player.getUniqueId());
    }

    public static PvpSession getSession(Player player) {
        return playerSessionMap.get(player.getUniqueId());
    }

    public static boolean isOwner(Player player) {
        return activeSession != null && activeSession.getOwner().equals(player);
    }

    public static boolean canJoin(Player player) {
        if (activeSession == null) return false;
        // Không cho phép join nếu đã bắt đầu hoặc đang đếm ngược
        if (activeSession.isStarted() || activeSession.isCountdownPhase()) {
            player.sendMessage("§cTrận PvP đã bắt đầu hoặc đang đếm ngược, không thể tham gia.");
            return false;
        }
        if (activeSession.isPublicRoom()) return true;
        return activeSession.getInvitedPlayers().contains(player);
    }

    /**
     * Xóa người chơi khỏi phiên PvP.
     * Phương thức này gọi PvpSession.removePlayer() để xử lý logic bên trong phiên
     * và sau đó hủy đăng ký người chơi khỏi bản đồ quản lý toàn cục.
     */
    public static void removePlayer(Player player) {
        if (activeSession != null) {
            // PvpSession.removePlayer() cũng sẽ xử lý việc xóa khỏi WorldGuard region
            activeSession.removePlayer(player);
            unregisterPlayer(player); // Xóa khỏi map quản lý của PvpSessionManager
        }
    }
    
    public static PvpSession getSession() {
    	return activeSession;
    }
}