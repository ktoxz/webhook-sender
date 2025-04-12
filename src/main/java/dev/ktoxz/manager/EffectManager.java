package dev.ktoxz.manager;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EffectManager {

    public static void showTradeComplete(Player player) {
        Location loc = player.getLocation().add(0, 1, 0); // hiệu ứng quanh người chơi

        // Hiện particle
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.3, 0.3, 0.3, 0.1);

        // Phát âm thanh
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.2f); // to, cao độ cao
    }
    
    public static void showTradeLeftover(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);

        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 20, 0.4, 0.4, 0.4, 0.01);
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 10, 0.2, 0.2, 0.2, 0.05);
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1f); // bass thấp
    }
    
    public static void showTeleportComplete(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);

        player.getWorld().spawnParticle(Particle.PORTAL, loc, 40, 0.6, 0.6, 0.6, 0.2);
        player.getWorld().spawnParticle(Particle.SPELL_MOB_AMBIENT, loc, 15, 0.3, 0.3, 0.3, 0.01);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 20, 0.2, 0.2, 0.2, 0.03);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);
    }
}
