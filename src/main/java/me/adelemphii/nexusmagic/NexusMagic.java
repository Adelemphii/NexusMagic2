package me.adelemphii.nexusmagic;

import me.adelemphii.nexusmagic.commands.SpellCommand;
import me.adelemphii.nexusmagic.events.AbnormalityListener;
import me.adelemphii.nexusmagic.events.DamageImmunityListener;
import me.adelemphii.nexusmagic.events.SpellEffectsListener;
import me.adelemphii.nexusmagic.events.SpellListener;
import me.adelemphii.nexusmagic.spells.SpellRegistry;
import me.adelemphii.nexusmagic.utility.ManaBar;
import me.adelemphii.nexusmagic.utility.ManaManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class NexusMagic extends JavaPlugin {

    private static NexusMagic instance;

    private static SpellRegistry spellRegistry = SpellRegistry.getInstance();
    private BukkitRunnable barRunnable;

    @Override
    public void onEnable() {
        instance = this;

        spellRegistry.registerSpells();

        getCommand("cast").setExecutor(new SpellCommand());
        getServer().getPluginManager().registerEvents(new SpellListener(), this);
        getServer().getPluginManager().registerEvents(new DamageImmunityListener(), this);
        getServer().getPluginManager().registerEvents(new SpellEffectsListener(), this);
        getServer().getPluginManager().registerEvents(new AbnormalityListener(), this);

        ManaManager.startManaRegen();
        barRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                ManaBar.updateAllManaBars();
            }
        };
        barRunnable.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            ManaBar.removeManaBar(player);
            player.removeMetadata("briskstep", this);

            if(player.hasMetadata("hugify-enlarged") || player.hasMetadata("shrinkify-enlarged")) {
                player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1D);
                player.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(0.6D);
                player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5D);
                player.removeMetadata("hugify-enlarged", this);
                player.removeMetadata("shrinkify-enlarged", this);
            }

            if(player.hasMetadata("nexusmagic-invisible")) {
                for(Player online : Bukkit.getOnlinePlayers()) {
                    online.showEntity(NexusMagic.getInstance(), player);
                }
                player.removeMetadata("nexusmagic-invisible", NexusMagic.getInstance());
            }
        }
        barRunnable.cancel();
    }

    public static SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    public static NexusMagic getInstance() {
        return instance;
    }
}
