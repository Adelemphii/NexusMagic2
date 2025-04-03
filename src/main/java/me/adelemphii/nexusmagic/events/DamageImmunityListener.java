package me.adelemphii.nexusmagic.events;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.adelemphii.nexusmagic.NexusMagic;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageImmunityListener implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getDamageSource();
        DamageType immunity = NexusMagic.getSpellRegistry().getImmunity(entity);

        if(immunity == null) {
            return;
        }

        if(source.getDamageType() == immunity) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        DamageType immunity = NexusMagic.getSpellRegistry().getImmunity(entity);
        if(immunity == null || immunity != DamageType.FALL) {
            return;
        }

        if(entity.isOnGround() && NexusMagic.getSpellRegistry().hasImmunityExpired(entity)) {
            NexusMagic.getSpellRegistry().removeImmunity(entity);
        }
    }
}
