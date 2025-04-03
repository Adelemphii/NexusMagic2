package me.adelemphii.nexusmagic.events;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.adelemphii.nexusmagic.NexusMagic;
import me.adelemphii.nexusmagic.spells.Spell;
import me.adelemphii.nexusmagic.spells.SpellRegistry;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Random;

public class SpellEffectsListener implements Listener {

    private final SpellRegistry spellRegistry = NexusMagic.getSpellRegistry();

    @EventHandler
    public void onPlayerhit(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        if (victim instanceof LivingEntity livingVictim && livingVictim.hasMetadata("briskstep")) {
            Spell briskStep = spellRegistry.getSpell("briskstep");

            Location originalLocation = livingVictim.getLocation();
            Random random = new Random();
            Location randomLocation = findRandomBlock(livingVictim);

            if (randomLocation != null && randomLocation.getBlock().getType().isAir()) {
                originalLocation.getWorld().playSound(originalLocation, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
                spawnSwirlingTeleportEffect(originalLocation, true);

                livingVictim.teleport(randomLocation);
                spawnSwirlingTeleportEffect(randomLocation, false);
                randomLocation.getWorld().playSound(randomLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, random.nextFloat(0.5f, 2));

                livingVictim.sendMessage(briskStep.getColor() + "You swiftly teleport to safety!");
            } else {
                livingVictim.sendMessage(ChatColor.RED + "Brisk Step failed. No safe spot was found!!");
            }

            livingVictim.removeMetadata("briskstep", NexusMagic.getInstance());
        }
    }

    private void spawnSwirlingTeleportEffect(Location location, boolean goingUp) {
        new BukkitRunnable() {
            double angle = 0;
            double height = 0;
            final double maxHeight = 2.5;

            @Override
            public void run() {
                if ((goingUp && height > maxHeight) || (!goingUp && height < -maxHeight)) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    double xOffset = Math.cos(angle) * 1.0;
                    double zOffset = Math.sin(angle) * 1.0;
                    Location particleLoc = location.clone().add(xOffset, height, zOffset);

                    location.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 0.2, 0.2, 0.2, 0.1);
                    location.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.05, 0.05, 0.05, 0.02);
                    location.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 3, 0.1, 0.1, 0.1, 0);

                    if (height < 0.5) {
                        location.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 20, 0.5, 0.5, 0.5, 0.2);
                    }
                }

                angle += Math.PI / 4;
                height += goingUp ? 0.6 : -0.6;

            }
        }.runTaskTimer(NexusMagic.getInstance(), 0L, 1L);
    }

    private Location findRandomBlock(LivingEntity livingVictim) {
        Location eyeLocation = livingVictim.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        World world = eyeLocation.getWorld();

        // Perform the raycast up to 16 blocks in the direction the player is looking
        RayTraceResult result = world.rayTraceBlocks(eyeLocation, direction, 16);

        if (result != null && result.getHitBlock() != null) {
            Location hitLocation = result.getHitPosition().toLocation(world).add(0, 1, 0); // Move on top of the block

            // Ensure the teleport location is safe
            if (world.getBlockAt(hitLocation).getType() == Material.AIR &&
                    world.getBlockAt(hitLocation.clone().add(0, 1, 0)).getType() == Material.AIR) {
                return hitLocation;
            }
            hitLocation.subtract(direction);
            return hitLocation;
        }
        return null;
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if(event.getFrom() == event.getTo()) return;
        LivingEntity afflicted = event.getEntity();
        if(spellRegistry.isAfflictedBy(afflicted, SpellRegistry.SpellEffects.STUNNED)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMove(PlayerMoveEvent event) {
        if(!event.hasChangedBlock()) return;
        LivingEntity afflicted = event.getPlayer();
        if(spellRegistry.isAfflictedBy(afflicted, SpellRegistry.SpellEffects.STUNNED)) {
            afflicted.sendMessage(ChatColor.RED + "You cannot move while stunned!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if(!(damager instanceof LivingEntity afflicted)) {
            return;
        }
        if(spellRegistry.isAfflictedBy(afflicted, SpellRegistry.SpellEffects.STUNNED) || spellRegistry.isAfflictedBy(afflicted, SpellRegistry.SpellEffects.STUNNED)) {
            event.setCancelled(true);
            afflicted.sendMessage(ChatColor.RED + "You cannot attack while stunned or polymorphed!");
        }
    }

}
