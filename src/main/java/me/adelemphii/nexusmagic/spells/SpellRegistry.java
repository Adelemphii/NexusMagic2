package me.adelemphii.nexusmagic.spells;

import me.adelemphii.nexusmagic.NexusMagic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpellRegistry {

    private final Map<UUID, Boolean> casting = new HashMap<>();
    private final Map<String, Spell> spells = new HashMap<>();

    private final Map<UUID, Integer> spellIndexMap = new HashMap<>();

    private final Map<UUID, Map<DamageType, Long>> damageImmunityMap = new HashMap<>();
    private final Map<UUID, Set<SpellEffects>> activeSpellEffects = new HashMap<>();

    private final Map<UUID, Long> spellCooldown = new HashMap<>(); // Cooldown for spells that could be really bad if spammed
    private final long COOLDOWN_TIME_MILLIS = 30000;

    private final Random random = new Random();

    public void registerSpells() {
        spells.put("flamelance", new Spell("Flamelance", ChatColor.of("#FF4500"), 25) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }
                caster.sendMessage(getColor() + "You conjure a blazing lance of fire!");
                consumeMana(caster);

                World world = caster.getWorld();
                Location start = caster.getEyeLocation();
                Vector direction = start.getDirection().normalize();

                // Spawn white sparkles at the initial cast location
                for (int i = 0; i < 5; i++) {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Location sparkleLocation = start.clone().add(random.nextFloat(-1, 1), random.nextFloat(-1, 1), random.nextFloat(-1, 1));
                    world.spawnParticle(Particle.FIREWORK, sparkleLocation, 2, 0.1, 0.1, 0.1, 0.01);
                }

                new BukkitRunnable() {
                    private int step = 0;
                    private final int maxSteps = 15;
                    private Location currentLocation = start.clone();

                    @Override
                    public void run() {
                        if (step >= maxSteps) {
                            cancel();
                            return;
                        }

                        currentLocation.add(direction);
                        world.spawnParticle(Particle.FLAME, currentLocation, 5, 0.1, 0.1, 0.1, 0.01);

                        /* if (currentLocation.clone().subtract(0, 1, 0).getBlock().getType() != Material.AIR && currentLocation.getBlock().getType() == Material.AIR) {
                            //currentLocation.getBlock().setType(Material.FIRE);
                        } else */
                        if(currentLocation.getBlock().getType() != Material.AIR) {
                            cancel();
                            return;
                        }

                        for (LivingEntity livingEntity : world.getNearbyLivingEntities(currentLocation, 1, 1, 1)) {
                            if (livingEntity.equals(caster)) continue;
                            if(!canBeTargeted(livingEntity)) continue;
                            if(!isCastingCosmetic(caster)) {
                                livingEntity.setFireTicks(60);
                                livingEntity.damage(8.0, caster);
                            }
                        }

                        step++;
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0L, 1L);

                world.playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0F, random.nextFloat(0.5f, 2f));
            }
        });
        spells.put("gust", new Spell("Gust", ChatColor.of("#ADD8E6"), 15) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }
                caster.sendMessage(getColor() + "You summon a powerful tunnel of wind!");
                consumeMana(caster);

                World world = caster.getWorld();
                Location start = caster.getEyeLocation();
                Vector direction = start.getDirection().normalize();

                for (int i = 1; i <= 10; i++) {
                    Location point = start.clone().add(direction.clone().multiply(i));
                    world.spawnParticle(Particle.CLOUD, point, 1, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.SNOWFLAKE, point, 1, 0.2, 0.2, 0.2, 0.02);

                    for (LivingEntity livingEntity : world.getNearbyLivingEntities(point, 1.5, 1.5, 1.5)) {
                        if (livingEntity.equals(caster)) continue;
                        if(!canBeTargeted(livingEntity)) continue;

                        float value = caster.isSneaking() ? -2f : 2f;
                        Vector push = direction.clone().multiply(value);
                        push.setY(Math.min(push.getY(), 1.0));
                        livingEntity.setVelocity(push);
                    }
                }

                world.playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, random.nextFloat(0.5f, 2f));
            }
        });
        spells.put("iceshard", new Spell("Ice Shard", ChatColor.of("#00BFFF"), 25) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }
                caster.sendMessage(getColor() + "You launch a piercing shard of ice!");
                consumeMana(caster);

                World world = caster.getWorld();
                Location start = caster.getEyeLocation();
                Vector direction = start.getDirection().normalize();

                start.getWorld().playSound(start, Sound.BLOCK_DECORATED_POT_SHATTER, 1f, random.nextFloat(0.5f, 2f));

                for (int i = 1; i <= 15; i++) {
                    Location point = start.clone().add(direction.clone().multiply(i));
                    if(point.getBlock().getType() != Material.AIR) {
                        return;
                    }

                    world.spawnParticle(Particle.FISHING, point, 10, 0.1, 0.1, 0.1, 0.05);

                    for (LivingEntity livingEntity : world.getNearbyLivingEntities(point, 1, 1, 1)) {
                        if (livingEntity.equals(caster)) continue;
                        if(!canBeTargeted(livingEntity)) continue;

                        if(!isCastingCosmetic(caster)) {
                            livingEntity.damage(4.0, caster);
                        }
                        world.playSound(livingEntity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, random.nextFloat(0.5f, 2f));
                        return;
                    }
                }
            }
        });
        spells.put("earthshatter", new Spell("Earth Shatter", ChatColor.of("#8B4513"), 75) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }

                caster.sendMessage(getColor() + "You summon a shockwave to shake the ground!");
                consumeMana(caster);

                Location start = caster.getLocation().clone();
                float yaw = start.getYaw();
                Vector direction = new Vector(Math.sin(Math.toRadians(yaw)), 0, -Math.cos(Math.toRadians(yaw))).normalize();

                Location currentLocation = start.clone().add(direction.multiply(-2));
                double distance = 10;
                double speed = 0.3;
                double soundInterval = 2.0;

                Sound[] sounds = {
                        Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
                        Sound.ENTITY_GENERIC_EXPLODE,
                        Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                        Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER
                };

                // Move the spell forward along the ground
                new BukkitRunnable() {
                    double traveled = 0;
                    int soundIndex = 0;

                    @Override
                    public void run() {
                        if (traveled >= distance) {
                            cancel();
                            return;
                        }

                        // Crawl over terrain
                        if (currentLocation.getBlock().getType() == Material.AIR) {
                            while (currentLocation.getBlock().getType() == Material.AIR) {
                                currentLocation.subtract(0, 1, 0);
                            }

                            if (currentLocation.getBlock().getType() != Material.AIR) {
                                currentLocation.add(0, 1, 0);
                            }
                        } else {
                            while (currentLocation.getBlock().getType() != Material.AIR) {
                                currentLocation.add(0, 1, 0);
                            }
                        }

                        BlockData blockData = Material.STONE.createBlockData();

                        caster.getWorld().spawnParticle(Particle.EXPLOSION, currentLocation, 5, 1.5, 0.3, 1.5, 0.1);
                        caster.getWorld().spawnParticle(Particle.BLOCK, currentLocation, 30, 1.5, 0.3, 1.5, 0.2, blockData);
                        caster.getWorld().spawnParticle(Particle.LARGE_SMOKE, currentLocation, 10, 1.5, 0.3, 1.5, 0.05);

                        for (Entity hitEntity : caster.getWorld().getNearbyEntities(currentLocation, 2, 3, 2)) {
                            if (hitEntity instanceof LivingEntity livingEntity && !hitEntity.equals(caster)) {
                                if(!canBeTargeted(livingEntity)) {
                                    continue;
                                }

                                Vector direction = livingEntity.getLocation().toVector().subtract(currentLocation.toVector()).normalize();
                                Vector knockback = direction.multiply(1.1);

                                livingEntity.setVelocity(knockback);
                                livingEntity.setVelocity(knockback.add(new Vector(0, 0.5, 0)));

                                if(isCastingCosmetic(caster)) {
                                    addImmunity(livingEntity, DamageType.FALL);
                                }

                                addAffliction(livingEntity, SpellEffects.KNOCKED_UP);

                                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1));

                                float yawDamageFrom = (float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()));

                                if(!isCastingCosmetic(caster)) {
                                    livingEntity.damage(10);
                                }
                                livingEntity.playHurtAnimation(yawDamageFrom);
                            }
                        }

                        for (Player nearbyPlayer : currentLocation.getWorld().getPlayers()) {
                            if (nearbyPlayer.getLocation().distance(currentLocation) < 20) {
                                nearbyPlayer.playEffect(currentLocation, Effect.STEP_SOUND, Material.STONE);
                            }
                        }

                        for(int x = -2; x < 2; x++) {
                            for(int z = -2; z < 2; z++) {
                                if(random.nextFloat() < 0.2f) {
                                    Location displayLocation = currentLocation.clone().add(x, -1 , z);

                                    BlockData displayData = displayLocation.getBlock().getBlockData();
                                    BlockDisplay display = displayLocation.getWorld().spawn(displayLocation, BlockDisplay.class, entity ->
                                            entity.setBlock(displayData));

                                    final int[] duration = {20}; // duration of half a revolution
                                    int totalTicks = duration[0];
                                    float upDownAmplitude = 0.5f; // maximum height the item will move up and down

                                    Matrix4f mat = new Matrix4f().scale(0.5f);
                                    Bukkit.getScheduler().runTaskTimer(NexusMagic.getInstance(), task -> {
                                        if (!display.isValid()) {
                                            task.cancel();
                                            return;
                                        }

                                        // Calculate vertical movement: sine wave for smooth up and down motion
                                        float progress = ((float) (totalTicks - duration[0])) / totalTicks;
                                        float yOffset = (float) Math.sin(progress * Math.PI * 2) * upDownAmplitude;

                                        display.setTransformationMatrix(mat.translate(0, yOffset, 0));

                                        display.setInterpolationDelay(0);
                                        display.setInterpolationDuration(duration[0]);

                                        if (duration[0] <= 0) {
                                            display.remove();
                                            task.cancel();
                                        }

                                        duration[0]--;
                                    }, 1, 1);
                                }
                            }
                        }

                        if (traveled >= soundInterval * (soundIndex + 1)) {
                            Sound randomSound = sounds[(int) (Math.random() * sounds.length)];
                            caster.getWorld().playSound(currentLocation, randomSound, 0.5f, random.nextFloat(0.5f, 2f));
                            soundIndex++;
                        }

                        currentLocation.add(direction.clone().multiply(speed));
                        traveled += speed;
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0L, 1L);
            }
        });
        spells.put("airlaunch", new Spell("Air Launch", ChatColor.of("#87CEEB"), 20) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }
                if(!caster.isOnGround()) {
                    caster.sendMessage(ChatColor.RED + "You must be on the ground to cast this spell!");
                    return;
                }

                caster.sendMessage(getColor() + "You summon a vortex of air to boost you forward!");
                consumeMana(caster);

                Location location = caster.getLocation();
                addImmunity(caster, DamageType.FALL);

                Vector push = location.getDirection().multiply(2.5);
                push.setY(Math.min(push.getY(), 0.6));
                caster.setVelocity(push);
                caster.getWorld().playSound(location, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, random.nextFloat(0.5f, 2f));
                caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 1, 0, 0.3, 0, 0.1);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(!caster.isValid()) {
                            cancel();
                            return;
                        }

                        if(caster.isOnGround()) {
                            damageImmunityMap.remove(caster.getUniqueId());
                            cancel();
                            return;
                        }

                        caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 3, 0, 0.3, 0, 0.1);
                    }

                    @Override
                    public synchronized void cancel() throws IllegalStateException {
                        super.cancel();
                        damageImmunityMap.remove(caster.getUniqueId());
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 3L, 1L);
            }
        });
        spells.put("polymorph", new Spell("Polymorph", ChatColor.of("#9370DB"), 80) {
            private final List<EntityType> ANIMALS = Arrays.asList(
                    EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                    EntityType.RABBIT, EntityType.CAT, EntityType.FOX, EntityType.FROG, EntityType.AXOLOTL
            );

            private final List<Sound> MORPH_SOUNDS = Arrays.asList(
                    Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED
            );

            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }

                LivingEntity target = getTargetEntity(caster);
                if (target == null) {
                    caster.sendMessage(ChatColor.RED + "No valid target found!");
                    return;
                }

                if(activeSpellEffects.containsKey(target.getUniqueId())
                        && activeSpellEffects.get(target.getUniqueId()).contains(SpellEffects.POLYMORPHED)) {
                    caster.sendMessage(ChatColor.RED + "Cannot cast polymorph on a polymorphed entity!");
                    return;
                }

                caster.sendMessage(getColor() + "A torrent of raging mana shoots forth from your being, wrapping itself around your target and twisting their form into that of another!");
                target.sendMessage(getColor() + "A torrent of raging mana wraps itself around your body as you feel your perception of the world twist, finding yourself in the body of a different creature!");
                consumeMana(caster);

                caster.getLocation().getWorld().playSound(
                        caster.getLocation(),
                        MORPH_SOUNDS.get(random.nextInt(MORPH_SOUNDS.size())),
                        1f,
                        random.nextFloat(0.5f, 2)
                );
                target.getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 15, 0.5, 0.5, 0.5);

                EntityType newType = ANIMALS.get(random.nextInt(ANIMALS.size()));

                Location originalLocation = target.getLocation();
                double health = target.getHealth();
                Component targetName = target.customName() != null ? target.customName() : target.name();

                LivingEntity newEntity = (LivingEntity) target.getWorld().spawnEntity(originalLocation, newType);

                addAffliction(target, SpellEffects.POLYMORPHED);
                addAffliction(newEntity, SpellEffects.POLYMORPHED);

                newEntity.customName(targetName);
                newEntity.setCustomNameVisible(true);

                newEntity.setAI(false);
                newEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
                newEntity.setHealth(health);
                newEntity.setMetadata("nexusmagic-nodrop", new FixedMetadataValue(NexusMagic.getInstance(), true));

                target.setMetadata("nexusmagic-invisible", new FixedMetadataValue(NexusMagic.getInstance(), true));
                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.hideEntity(NexusMagic.getInstance(), target);
                }

                // Sync the movements of the polymorphed entity
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!newEntity.isValid() || !target.isValid()) {
                            cancel();
                            return;
                        }

                        target.setHealth(newEntity.getHealth());
                        newEntity.teleport(target.getLocation());
                        newEntity.setRotation(target.getLocation().getYaw(), target.getLocation().getPitch());
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 1L, 1L);

                // Revert the polymorph after 10 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        removeAffliction(newEntity, SpellEffects.POLYMORPHED);
                        removeAffliction(target, SpellEffects.POLYMORPHED);

                        target.getLocation().getWorld().playSound(
                                target.getLocation(),
                                Sound.ENTITY_WIND_CHARGE_WIND_BURST,
                                1f,
                                random.nextFloat(0.5f, 2f)
                        );
                        target.getLocation().getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 15, 0.5, 0.5, 0.5);
                        target.sendMessage(getColor() + "Finally, freed from the prison that was polymorph...");

                        newEntity.remove();

                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.showEntity(NexusMagic.getInstance(), target);
                        }
                        target.removeMetadata("nexusmagic-invisible", NexusMagic.getInstance());
                    }
                }.runTaskLater(NexusMagic.getInstance(), 200L); // 200 ticks = 10 seconds
            }

            private LivingEntity getTargetEntity(LivingEntity caster) {
                if(caster.isSneaking()) {
                    return caster;
                }

                RayTraceResult result = caster.getWorld().rayTraceEntities(
                        caster.getEyeLocation(), caster.getLocation().getDirection(), 20,
                        entity -> entity instanceof LivingEntity && !entity.equals(caster)
                );
                if(result == null || result.getHitEntity() == null) {
                    return null;
                }
                if(!canBeTargeted((LivingEntity) result.getHitEntity())) {
                    return null;
                }

                return (LivingEntity) result.getHitEntity();
            }
        });
        /*spells.put("briskstep", new Spell("Brisk Step", ChatColor.of("#32CD32"), 75) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }

                Location start = caster.getLocation();
                Vector direction = start.getDirection().normalize();

                Location current = start.clone();


                for(int i = 0; i < 12; i++) {
                    int distanceTravelledVertically = 0;

                    while(current.getBlock().isSolid()) {
                        if(distanceTravelledVertically > 3) {
                            break;
                        }
                        current.add(0, 1, 0);
                        distanceTravelledVertically++;
                    }
                    while(!current.getBlock().isSolid() && !current.clone().subtract(0, 1, 0).getBlock().isSolid()) {
                        if(distanceTravelledVertically > 3) {
                            break;
                        }
                        current.subtract(0, 1, 0);
                        distanceTravelledVertically++;
                    }

                    if(Math.abs(current.getY() - start.getY()) > 1) {
                        break;
                    } else {
                        current.add(direction.clone().multiply(1));
                    }
                }
                caster.teleport(current);
            }
        }); */
        spells.put("beesurge", new Spell("Bee Surge", ChatColor.of("#FFD700"), 95) {
            @Override
            public void cast(LivingEntity caster) {
                caster.sendMessage(getColor() + "Unfortunately due to server issues, this has been disabled...");
                return;
                /*
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }
                if(isCastingCosmetic(caster)) {
                    caster.sendMessage(ChatColor.RED + "Unfortunately due to the nature of this spell, it cannot be cast while in cosmetic mode...");
                    return;
                }

                String cooldown = cooldown(caster);
                if(cooldown != null) {
                    caster.sendMessage(cooldown);
                    return;
                }

                caster.sendMessage(getColor() + "You begin charging a swarm of bees! (Hold sneak to charge, let go to release!)");
                consumeMana(caster);
                spellCooldown.put(caster.getUniqueId(), System.currentTimeMillis());

                int maxChargeDistance = 15;
                final double[] chargeDistance = {0};

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!caster.isSneaking()) {
                            launchBeeSpell(caster, chargeDistance[0]);
                            cancel();
                            return;
                        }

                        if (chargeDistance[0] < maxChargeDistance) {
                            chargeDistance[0] += 0.5;
                        }

                        double percentage = (chargeDistance[0] / maxChargeDistance) * 100;
                        caster.sendActionBar(Component.text("Charging: " + (int) percentage + "%").color(NamedTextColor.YELLOW));

                        Location targetLocation = caster.getEyeLocation().add(caster.getEyeLocation().getDirection().multiply(chargeDistance[0]));
                        caster.getWorld().spawnParticle(Particle.END_ROD, targetLocation, 1, 0.2, 0.2, 0.2, 0);

                        spawnChargingParticles(caster, chargeDistance[0]);

                        float pitch = 1.0f + (float) (chargeDistance[0] / maxChargeDistance) * 0.5f;
                        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, pitch);
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0, 1); */
            }

            private void spawnChargingParticles(LivingEntity caster, double chargeDistance) {
                double radius = 0.5 + (chargeDistance / 15);
                int particleCount = (int) (5 + (chargeDistance / 3));

                Location casterLocation = caster.getLocation();
                for (int i = 0; i < particleCount; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLocation = casterLocation.clone().add(x, 1.5, z);
                    caster.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.YELLOW, 1));
                }
            }

            private void launchBeeSpell(LivingEntity caster, double chargeDistance) {
                double initialSpeed = chargeDistance * 0.5;
                Vector direction = caster.getEyeLocation().getDirection();

                Vector velocity = direction.normalize().multiply(initialSpeed);

                double gravity = 0.04;
                double timeStep = 0.1;

                Location startLocation = caster.getEyeLocation().clone();

                new BukkitRunnable() {
                    double time = 0;
                    Location currentLocation = startLocation.clone();

                    @Override
                    public void run() {
                        double x = currentLocation.getX() + velocity.getX() * timeStep;
                        double y = currentLocation.getY() + velocity.getY() * timeStep - (gravity * time * time);
                        double z = currentLocation.getZ() + velocity.getZ() * timeStep;

                        currentLocation = new Location(currentLocation.getWorld(), x, y, z);

                        caster.getWorld().spawnParticle(Particle.DUST, currentLocation, 3, new Particle.DustOptions(Color.YELLOW, 1));

                        if (currentLocation.getBlock().getType().isSolid() || currentLocation.getY() <= 0) {
                            spawnBees(currentLocation.add(0, 1, 0), caster);
                            cancel();
                        }

                        time += timeStep;
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0, 1);
            }

            private void spawnBees(Location location, LivingEntity caster) {
                List<Bee> bees = new ArrayList<>();

                for (int i = 0; i < 10; i++) {
                    Bee bee = (Bee) location.getWorld().spawnEntity(location, EntityType.BEE);

                    LivingEntity target = findClosestTarget(bee, caster);
                    if (target != null) {
                        bee.setTarget(target);
                    }

                    bees.add(bee);
                }

                Bee queenBee = (Bee) location.getWorld().spawnEntity(location, EntityType.BEE);
                queenBee.customName(Component.text("Queen Bee").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
                queenBee.setCustomNameVisible(true);

                queenBee.setGlowing(true);
                queenBee.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(2D);
                queenBee.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40);
                queenBee.setHealth(40);

                LivingEntity queenTarget = findClosestTarget(queenBee, caster);
                if (queenTarget != null) {
                    queenBee.setTarget(queenTarget);
                }

                location.getWorld().playSound(location, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.0f, random.nextFloat(0.5f, 2f));

                // Special Abilities
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(!queenBee.isValid()) {
                            cancel();
                            for (Bee bee : bees) {
                                bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 1);
                                bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_DEATH, 1.0f, random.nextFloat(0.5f, 2f));

                                bee.remove();
                            }
                            return;
                        }

                        // Healing Aura: Heal nearby bees
                        for (Entity entity : queenBee.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof Bee nearbyBee) {
                                double maxHealth = nearbyBee.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                                if (nearbyBee != queenBee && nearbyBee.getHealth() < maxHealth) {
                                    nearbyBee.setHealth(Math.min(nearbyBee.getHealth() + 1, maxHealth)); // Heal 2 health per tick
                                }
                            }
                        }

                        // Damage Reduction: Give nearby bees damage reduction
                        for (Entity entity : queenBee.getNearbyEntities(10, 10, 10)) {
                            if (entity instanceof Bee nearbyBee) {
                                if (nearbyBee != queenBee) {
                                    nearbyBee.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20, 1));
                                }
                            }
                        }

                        // Summon Additional Bees periodically
                        if (Math.random() < 0.1) { // 10% chance to spawn a new bee every tick
                            Bee newBee = (Bee) queenBee.getWorld().spawnEntity(queenBee.getLocation(), EntityType.BEE);
                            LivingEntity target = findClosestTarget(newBee, caster);
                            if (target != null) {
                                newBee.setAnger(200);
                                newBee.setHasStung(false);
                                newBee.setTarget(target);
                            }
                            bees.add(newBee);
                        }

                        // Poison Attack: Apply poison to nearby enemies (if the Queen Bee attacks)
                        if (queenBee.getTarget() != null) {
                            LivingEntity target = queenBee.getTarget();
                            if (target != null && queenBee.getLocation().distance(target.getLocation()) < 5) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10, 1));
                            }
                        }

                        if(queenBee.getTarget() == null) {
                            LivingEntity target = findClosestTarget(queenBee, caster);
                            if(target != null) {
                                queenBee.setAnger(200);
                                queenBee.setHasStung(false);
                                queenBee.setTarget(target);

                                for(Bee bee : bees) {
                                    bee.setAnger(200);
                                    bee.setHasStung(false);
                                    bee.setTarget(target);
                                }
                            }
                        }
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0, 20);

                bees.add(queenBee);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Bee bee : bees) {
                            bee.getWorld().spawnParticle(Particle.CLOUD, bee.getLocation(), 1);
                            bee.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_DEATH, 1.0f, random.nextFloat(0.5f, 2f));

                            bee.remove();
                        }
                    }
                }.runTaskLater(NexusMagic.getInstance(), 30 * 20);
            }

            private LivingEntity findClosestTarget(Bee bee, LivingEntity caster) {
                double closestDistance = Double.MAX_VALUE;
                LivingEntity closestEntity = null;

                for (Entity entity : bee.getNearbyEntities(15, 15, 15)) {
                    if (entity instanceof LivingEntity && entity != caster && !(entity instanceof Bee)) {
                        double distance = bee.getLocation().distance(entity.getLocation());
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestEntity = (LivingEntity) entity;
                        }
                    }
                }

                return closestEntity;
            }
        });
        spells.put("hugify", new Spell("Hugify", ChatColor.of("#FF69B4"), 35) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }

                if (caster.hasMetadata("hugify-enlarged") || caster.hasMetadata("shrinkify-enlarged")) {
                    caster.sendMessage(getColor() + "Magic already flows within you. You are as grand as can be!");
                    return;
                }

                caster.sendMessage(getColor() + "A shimmering light envelops you... Your form begins to swell with radiant power!");

                // Play sound for Hugify spell cast
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, random.nextFloat(0.5f, 2f));

                caster.setMetadata("hugify-enlarged", new FixedMetadataValue(NexusMagic.getInstance(), true));

                caster.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(3D);
                caster.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(2D);
                caster.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(9D);

                final int spellDuration = 2400; // 2400 ticks = 2 minutes
                new BukkitRunnable() {
                    int timeLeft = spellDuration;

                    @Override
                    public void run() {
                        int secondsLeft = timeLeft / 20;  // Convert ticks to seconds
                        caster.sendActionBar(Component.text("Hugify: " + secondsLeft + "s remaining").color(NamedTextColor.YELLOW));

                        timeLeft--;

                        if (timeLeft <= 0) {
                            caster.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1D);
                            caster.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(0.6D);
                            caster.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5D);

                            caster.removeMetadata("hugify-enlarged", NexusMagic.getInstance());
                            caster.sendMessage(getColor() + "The glowing aura fades, and you return to your usual form. Your magic lingers for just a moment more.");

                            // Play sound when Hugify spell ends
                            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, random.nextFloat(0.5f, 2f));  // You can replace this sound as well
                            cancel();
                        }
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0, 1); // Runs every tick (interval = 1)
            }
        });
        spells.put("shrinkify", new Spell("Shrinkify", ChatColor.of("#ADD8E6"), 35) {
            @Override
            public void cast(LivingEntity caster) {
                String castError = canCast(caster);
                if (castError != null) {
                    caster.sendMessage(castError);
                    return;
                }

                if (caster.hasMetadata("shrinkify-enlarged") || caster.hasMetadata("shrinkify-enlarged")) {
                    caster.sendMessage(getColor() + "Your form has already been altered by magic. You are as small as can be!");
                    return;
                }

                caster.sendMessage(getColor() + "A flicker of ethereal magic surrounds you... Your form begins to shrink and become more compact!");

                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, random.nextFloat(0.5f, 2f));

                caster.setMetadata("shrinkify-enlarged", new FixedMetadataValue(NexusMagic.getInstance(), true));

                caster.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(0.33D);
                caster.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(0.2D);
                caster.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(1.5D);

                final int spellDuration = 2400; // 2400 ticks = 2 minutes
                new BukkitRunnable() {
                    int timeLeft = spellDuration;

                    @Override
                    public void run() {
                        int secondsLeft = timeLeft / 20;
                        caster.sendActionBar(Component.text("Shrinkify: " + secondsLeft + "s remaining").color(NamedTextColor.AQUA));

                        timeLeft--;

                        if (timeLeft <= 0) {
                            // Revert to normal size after the spell ends
                            caster.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(1D);
                            caster.getAttribute(Attribute.GENERIC_STEP_HEIGHT).setBaseValue(0.6D);
                            caster.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5D);

                            caster.removeMetadata("shrinkify-enlarged", NexusMagic.getInstance());
                            caster.sendMessage(getColor() + "The magical shrinking effect fades, and you return to your usual size.");

                            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, random.nextFloat(0.5f, 2f));
                            cancel();
                        }
                    }
                }.runTaskTimer(NexusMagic.getInstance(), 0, 1);
            }
        });
    }

    public Spell getSpell(String name) {
        return spells.get(name.toLowerCase());
    }

    public Map<String, Spell> getSpells() {
        return spells;
    }

    public Map<UUID, Map<DamageType, Long>> getDamageImmunityMap() {
        return damageImmunityMap;
    }

    public DamageType getImmunity(Entity entity) {
        Map<DamageType, Long> immunityMap = damageImmunityMap.get(entity.getUniqueId());

        if (immunityMap == null || immunityMap.isEmpty()) {
            return null;
        }

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<DamageType, Long> entry : immunityMap.entrySet()) {
            if (entry.getValue() + 1000L <= currentTime) {
                immunityMap.remove(entry.getKey());
                return null;
            }
            return entry.getKey();
        }

        return null;
    }

    public boolean hasImmunityExpired(Entity entity) {
        Map<DamageType, Long> immunityMap = damageImmunityMap.get(entity.getUniqueId());

        if (immunityMap == null || immunityMap.isEmpty()) {
            return true;
        }

        long currentTime = System.currentTimeMillis();

        for (Long expiryTime : immunityMap.values()) {
            if (expiryTime + 1000L > currentTime) {
                return false;
            }
        }

        return true;
    }

    public void addImmunity(LivingEntity entity, DamageType immunity) {
        UUID entityId = entity.getUniqueId();
        Map<DamageType, Long> immunityMap = damageImmunityMap.getOrDefault(entityId, new HashMap<>());

        immunityMap.put(immunity, System.currentTimeMillis());
        damageImmunityMap.put(entityId, immunityMap);
    }

    public void removeImmunity(Entity entity) {
        damageImmunityMap.remove(entity.getUniqueId());
    }

    public String cooldown(LivingEntity caster) {
        long castTime = spellCooldown.getOrDefault(caster.getUniqueId(), 0L);
        long timeElapsed = System.currentTimeMillis() - castTime;

        if (timeElapsed < COOLDOWN_TIME_MILLIS) {
            long timeLeft = (COOLDOWN_TIME_MILLIS - timeElapsed) / 1000; // Convert to seconds
            return ChatColor.RED + "You are on a cooldown for 'Huge Spells'! Wait " + timeLeft + "s.";
        }

        return null;
    }

    public boolean toggleCasting(Entity entity, boolean cosmetic) {
        if(casting.remove(entity.getUniqueId()) != null) {
            return false;
        }
        casting.put(entity.getUniqueId(), cosmetic);
        return true;
    }

    public boolean isCasting(Entity entity) {
        return casting.containsKey(entity.getUniqueId());
    }

    public Map<UUID, Boolean> getCasting() {
        return casting;
    }

    public boolean isCastingCosmetic(LivingEntity entity) {
        return casting.getOrDefault(entity.getUniqueId(), false);
    }

    public Set<SpellEffects> getAfflictions(LivingEntity entity) {
        return activeSpellEffects.get(entity.getUniqueId());
    }

    public boolean isAfflictedBy(LivingEntity entity, SpellEffects effect) {
        return activeSpellEffects.get(entity.getUniqueId()) != null && activeSpellEffects.get(entity.getUniqueId()).contains(effect);
    }

    public void addAffliction(LivingEntity entity, SpellEffects affliction) {
        Set<SpellEffects> afflictions = activeSpellEffects.getOrDefault(entity.getUniqueId(), new HashSet<>());
        afflictions.add(affliction);

        activeSpellEffects.put(entity.getUniqueId(), afflictions);
    }

    public void removeAffliction(LivingEntity entity, SpellEffects affliction) {
        Set<SpellEffects> afflictions = activeSpellEffects.get(entity.getUniqueId());
        if(afflictions == null) return;

        afflictions.remove(affliction);

        activeSpellEffects.put(entity.getUniqueId(), afflictions);
    }

    public int getSpellIndex(LivingEntity caster) {
        return spellIndexMap.getOrDefault(caster.getUniqueId(), 0);
    }

    public void setSpellIndex(LivingEntity caster, int index) {
        spellIndexMap.put(caster.getUniqueId(), index);
    }

    public void removeSpellIndex(LivingEntity caster) {
        spellIndexMap.remove(caster.getUniqueId());
    }

    public Spell getCurrentSpell(LivingEntity caster ) {
        List<String> spellList = new ArrayList<>(getSpells().keySet());
        if (spellList.isEmpty()) return null;

        int index = getSpellIndex(caster);
        String spellName = spellList.get(index);

        return getSpell(spellName);
    }

    public static SpellRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final SpellRegistry INSTANCE = new SpellRegistry();
    }

    public enum SpellEffects {
        STUNNED(false),
        POLYMORPHED(false),
        KNOCKED_UP(true);

        private final boolean canCast;

        SpellEffects(boolean canCast) {
            this.canCast = canCast;
        }

        public boolean canCast() {
            return canCast;
        }
    }
}
