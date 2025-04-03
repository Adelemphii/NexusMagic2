package me.adelemphii.nexusmagic.events;

import me.adelemphii.nexusmagic.NexusMagic;
import me.adelemphii.nexusmagic.spells.Spell;
import me.adelemphii.nexusmagic.spells.SpellRegistry;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpellListener implements Listener {

    private final SpellRegistry spellRegistry = NexusMagic.getSpellRegistry();

    private final List<String> spellList = new ArrayList<>(NexusMagic.getSpellRegistry().getSpells().keySet());

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if(event.getHand() != EquipmentSlot.HAND) return;
        if(!spellRegistry.isCasting(event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        for(ItemStack item : player.getInventory().getArmorContents()) {
            if(item != null) {
                player.sendMessage(ChatColor.RED + "You try to muster your magical energies, yet you fail to gather mana... must be the armor.");
                return;
            }
        }

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && player.isSneaking()) {
            int nextIndex = (spellRegistry.getSpellIndex(player) + 1) % spellList.size();
            spellRegistry.setSpellIndex(player, nextIndex);

            Spell spell = spellRegistry.getCurrentSpell(player);
            if (spell != null) {
                player.sendMessage(spell.getColor() + "Selected spell: " + spell.getName() + ", MC: " + spell.getManaCost());
            }

            event.setCancelled(true);
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Spell spell = spellRegistry.getCurrentSpell(player);

            if (spell != null) {
                spell.cast(player);
            }

            event.setCancelled(true);
        }
    }
}
