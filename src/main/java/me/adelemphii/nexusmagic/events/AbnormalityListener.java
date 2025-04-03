package me.adelemphii.nexusmagic.events;

import me.adelemphii.nexusmagic.NexusMagic;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class AbnormalityListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(player.hasMetadata("briskstep")) {
            player.removeMetadata("briskstep", NexusMagic.getInstance());
        }

        if(player.hasMetadata("hugify-enlarged") || player.hasMetadata("shrinkify-enlarged")) {
            player.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1D);
            player.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(0.6D);
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5D);
            player.removeMetadata("hugify-enlarged", NexusMagic.getInstance());
            player.removeMetadata("shrinkify-enlarged", NexusMagic.getInstance());
        }

        if(player.hasMetadata("nexusmagic-invisible")) {
            for(Player online : Bukkit.getOnlinePlayers()) {
                online.showEntity(NexusMagic.getInstance(), player);
            }
            player.removeMetadata("nexusmagic-invisible", NexusMagic.getInstance());
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if(entity.hasMetadata("nexusmagic-nodrop")) {
            event.getDrops().clear();
        }
    }
}
