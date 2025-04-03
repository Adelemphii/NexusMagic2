package me.adelemphii.nexusmagic.utility;

import me.adelemphii.nexusmagic.NexusMagic;
import me.adelemphii.nexusmagic.spells.Spell;
import me.adelemphii.nexusmagic.spells.SpellRegistry;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ManaBar {

    private static final SpellRegistry spellRegistry = NexusMagic.getSpellRegistry();
    private static final Map<Player, BossBar> playerBossBars = new HashMap<>();

    private static final String BAR_TEXT = "Selected Spell: %s | Mana: %s %s";

    public static void updateManaBar(Player player) {
        if(!NexusMagic.getSpellRegistry().isCasting(player)) {
            removeManaBar(player);
            return;
        }

        Spell selectedSpell = spellRegistry.getCurrentSpell(player);
        int mana = ManaManager.getMana(player);
        float progress = mana / 100.0f;

        String cosmetic = spellRegistry.isCastingCosmetic(player) ? "| NO DAMAGE" : "";

        if (!playerBossBars.containsKey(player)) {
            BossBar bossBar = Bukkit.createBossBar(
                    selectedSpell.getColor() + BAR_TEXT.formatted(
                            selectedSpell.getName(),
                            mana,
                            cosmetic
                    ),
                    BarColor.BLUE, BarStyle.SOLID);
            bossBar.addPlayer(player);
            playerBossBars.put(player, bossBar);
        }

        BossBar bossBar = playerBossBars.get(player);
        bossBar.setProgress(progress);
        bossBar.setTitle(
                selectedSpell.getColor() + BAR_TEXT.formatted(
                        selectedSpell.getName(),
                        mana,
                        cosmetic
                )
        );
    }

    public static void updateAllManaBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateManaBar(player);
        }
    }

    public static void removeManaBar(Player player) {
        if (playerBossBars.containsKey(player)) {
            BossBar bossBar = playerBossBars.get(player);
            bossBar.removePlayer(player);
            playerBossBars.remove(player);
        }
    }
}
