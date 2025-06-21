package dev.ktoxz.pvp;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    	if(activeSession == null) return false;
    	return activeSession.isStarted();
    }

    public static PvpSession getActiveSession() {
        return activeSession;
    }

    public static void createSession(Player owner, boolean isPublic) {
        if (activeSession != null) return;
        activeSession = new PvpSession(plugin, owner, isPublic);
        registerPlayer(owner, activeSession);
    }
    
    

    public static void closeSession() {
    	PvpEventManager.clearEvents();
        if (activeSession != null) {
        	activeSession.destroyChests();
            activeSession.cancelSessionTimeout(); // Hủy tác vụ timeout khi đóng phiên
            for (Player p : activeSession.getPlayers()) {
                unregisterPlayer(p);
            }
        }
        activeSession = null;
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

    public static boolean hasStarted(Player player) {
        PvpSession session = playerSessionMap.get(player.getUniqueId());
        return session != null && session.isStarted();
    }

    public static PvpSession getSession(Player player) {
        return playerSessionMap.get(player.getUniqueId());
    }

    public static boolean isOwner(Player player) {
        return activeSession != null && activeSession.getOwner().equals(player);
    }

    public static boolean canJoin(Player player) {
        if (activeSession == null) return false;
        if (activeSession.isPublicRoom()) return true;
        return activeSession.getInvitedPlayers().contains(player);
    }

    public static void removePlayer(Player player) {
        if (activeSession != null) {
            activeSession.removePlayer(player);
            unregisterPlayer(player);

//            if (activeSession.getPlayers().size() <= 1) {
//                activeSession.broadcast("§cKhông đủ người chơi, phòng PvP đã bị hủy.");
//                closeSession();
//            }
        }
    }
    
    public static PvpSession getSession() {
    	return activeSession;
    }
}