package dev.ktoxz.pvp.event.impl;

import dev.ktoxz.pvp.event.PvpEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class EffectEvent extends PvpEvent {

    private static final Random random = new Random();

    // Định nghĩa cấu trúc cho từng hiệu ứng
    private record PotionEffectDefinition(PotionEffectType type, int durationSeconds, int amplifier, String message) {}

    // Danh sách tất cả các hiệu ứng có thể xảy ra
    private static final List<PotionEffectDefinition> ALL_EFFECTS = List.of(
            // Buffs
            new PotionEffectDefinition(PotionEffectType.SPEED, 20, 1, "[BUFF] +Speed II trong 20s!"),
            new PotionEffectDefinition(PotionEffectType.STRENGTH, 20, 0, "[BUFF] +Strength I trong 20s!"),
            new PotionEffectDefinition(PotionEffectType.REGENERATION, 15, 1, "[BUFF] +Regeneration II trong 15s!"),
            new PotionEffectDefinition(PotionEffectType.JUMP_BOOST, 20, 2, "[BUFF] +Jump Boost III trong 20s!"),
            new PotionEffectDefinition(PotionEffectType.RESISTANCE, 20, 1, "[BUFF] +Resistance II trong 20s!"),
            new PotionEffectDefinition(PotionEffectType.STRENGTH, 10, 4, "[BUFF] +Strength V trong 10s! (Hiếm)"), // Strength V (rare)

            // Debuffs
            new PotionEffectDefinition(PotionEffectType.SLOWNESS, 10, 0, "[DEBUFF] -Slowness I trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.WEAKNESS, 10, 1, "[DEBUFF] -Weakness II trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.BLINDNESS, 7, 0, "[DEBUFF] -Blindness trong 7s!"),
            new PotionEffectDefinition(PotionEffectType.WITHER, 10, 0, "[DEBUFF] -Wither trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.POISON, 10, 0, "[DEBUFF] -Poison trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.HUNGER, 10, 0, "[DEBUFF] -Hunger trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.NAUSEA, 10, 0, "[DEBUFF] -Nausea trong 10s!"),
            new PotionEffectDefinition(PotionEffectType.SLOW_FALLING, 15, 0, "[DEBUFF] -Slow Falling trong 15s!"),
            new PotionEffectDefinition(PotionEffectType.INSTANT_DAMAGE, 1, 0, "[DEBUFF] -Instant Damage!")
    );

    public EffectEvent(Plugin plugin) {
        super("Potion Effect", "Hiệu ứng ngẫu nhiên cho từng người", plugin);
    }

    @Override
    public void trigger(Set<Player> players) {
        if (ALL_EFFECTS.isEmpty()) {
            plugin.getLogger().warning("No potion effects defined for EffectEvent.");
            return;
        }

        // Chọn ngẫu nhiên một hiệu ứng từ danh sách
        PotionEffectDefinition chosenEffect = ALL_EFFECTS.get(random.nextInt(ALL_EFFECTS.size()));

        // Áp dụng hiệu ứng cho tất cả người chơi
        applyEffect(players, chosenEffect.type(), chosenEffect.durationSeconds() * 20, chosenEffect.amplifier(), chosenEffect.message());
    }

    // Helper method để áp dụng hiệu ứng và thông báo (tương tự applyEffect cũ)
    private void applyEffect(Set<Player> players, PotionEffectType type, int durationTicks, int amplifier, String message) {
        for (Player p : players) {
            // Kiểm tra nếu người chơi online trước khi áp dụng hiệu ứng
            if (p.isOnline()) {
                p.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
            }
        }
        broadcastActionBar(players, message);
    }

	@Override
	public void onEndMatch() {
		// TODO Auto-generated method stub
		
	}
}