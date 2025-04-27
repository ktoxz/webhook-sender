package dev.ktoxz.manager;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EffectManager {

    public static void showTradeComplete(Player player) {
        Location loc = player.getLocation().add(0, 1, 0); // hiệu ứng quanh người chơi

        // Hiện particle
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc, 20, 0.3, 0.3, 0.3, 0.1);

        // Phát âm thanh
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.2f); // to, cao độ cao
    }
    
    public static void showTradeLeftover(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);

        player.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.4, 0.4, 0.4, 0.01);
        player.getWorld().spawnParticle(Particle.WITCH, loc, 10, 0.2, 0.2, 0.2, 0.05);
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1f); // bass thấp
    }
    
    public static void showTeleportComplete(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
    }
}
