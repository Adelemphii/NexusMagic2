package me.adelemphii.nexusmagic.spells;

import me.adelemphii.nexusmagic.NexusMagic;
import me.adelemphii.nexusmagic.utility.ManaManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Set;

public abstract class Spell {

    private final String name;
    private final ChatColor color;
    private final int manaCost;

    public Spell(String name, ChatColor color, int manaCost) {
        this.name = name;
        this.color = color;
        this.manaCost = manaCost;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public int getManaCost() {
        return manaCost;
    }

    public abstract void cast(LivingEntity caster);

    public boolean hasMana(LivingEntity caster) {
        return ManaManager.getMana(caster) >= manaCost;
    }

    public boolean isAfflicted(LivingEntity entity) {
        Set<SpellRegistry.SpellEffects> spellEffects = NexusMagic.getSpellRegistry().getAfflictions(entity);
        if(spellEffects == null) {
            return false;
        }
        for(SpellRegistry.SpellEffects effect : spellEffects) {
            if(!effect.canCast()) {
                return true;
            }
        }
        return false;
    }

    public String canCast(LivingEntity entity) {
        if(!hasMana(entity)) {
            return ErrorMessage.NOT_ENOUGH_MANA.getMessage();
        }
        if(isAfflicted(entity)) {
            return ErrorMessage.AFFLICTED.getMessage();
        }
        return null;
    }

    public boolean canBeTargeted(LivingEntity entity) {
        return entity.getType() != EntityType.ARMOR_STAND;
    }

    public void consumeMana(LivingEntity entity) {
        ManaManager.subtractMana(entity, manaCost);
    }

    private enum ErrorMessage {
        NOT_ENOUGH_MANA(ChatColor.RED + "Not enough mana!"),
        AFFLICTED(ChatColor.RED + "You cannot cast while afflicted with an effect!");

        private final String message;

        ErrorMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
