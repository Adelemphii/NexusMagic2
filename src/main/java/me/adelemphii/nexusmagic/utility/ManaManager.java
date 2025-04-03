package me.adelemphii.nexusmagic.utility;

import me.adelemphii.nexusmagic.NexusMagic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class ManaManager {
    private static final Map<LivingEntity, Integer> playerMana = new HashMap<>();
    private static final int MAX_MANA = 100;
    private static final int REGENERATION_RATE = 5; // Mana regenerated per tick interval
    private static final int REGENERATION_INTERVAL = 10; // Ticks, 20 ticks = 1 second

    public static int getMana(LivingEntity entity) {
        return playerMana.getOrDefault(entity, MAX_MANA);
    }

    public static void setMana(LivingEntity entity, int mana) {
        if (mana > MAX_MANA) {
            mana = MAX_MANA;
        } else if (mana < 0) {
            mana = 0;
        }
        playerMana.put(entity, mana);
    }

    public static void addMana(LivingEntity entity, int amount) {
        int newMana = getMana(entity) + amount;
        setMana(entity, newMana);
    }

    public static void subtractMana(LivingEntity entity, int amount) {
        int newMana = getMana(entity) - amount;
        setMana(entity, newMana);
    }

    public static void startManaRegen() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity entity : playerMana.keySet()) {
                    int currentMana = getMana(entity);
                    if (currentMana < MAX_MANA) {
                        int newMana = currentMana + REGENERATION_RATE;
                        setMana(entity, newMana);
                    }
                }
            }
        }.runTaskTimerAsynchronously(NexusMagic.getInstance(), 0L, REGENERATION_INTERVAL);
    }
}
