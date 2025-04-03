package me.adelemphii.nexusmagic.commands;

import me.adelemphii.nexusmagic.NexusMagic;
import me.adelemphii.nexusmagic.spells.SpellRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpellCommand implements CommandExecutor {

    private final SpellRegistry spellRegistry = NexusMagic.getSpellRegistry();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can cast spells!");
            return true;
        }

        if (args.length == 0) {
            String message = spellRegistry.toggleCasting(player, true)
                    ? ChatColor.GREEN + "You are now in spellcasting mode!"
                    : ChatColor.RED + "You are no longer in spellcasting mode!";
            player.sendMessage(message);
            return true;
        }

        String flag = args[0];
        if(flag.equalsIgnoreCase("-cosmetic") || flag.equalsIgnoreCase("-c")) {
            String message = spellRegistry.toggleCasting(player, true)
                    ? ChatColor.GREEN + "You are now in spellcasting mode!"
                    : ChatColor.RED + "You are no longer in spellcasting mode!";

            player.sendMessage(message);
        }
        if(flag.equalsIgnoreCase("-damage") || flag.equalsIgnoreCase("-d")) {
            String message = spellRegistry.toggleCasting(player, false)
                    ? ChatColor.GREEN + "You are now in spellcasting mode!"
                    : ChatColor.RED + "You are no longer in spellcasting mode!";

            player.sendMessage(message);
        }
        return true;
    }
}
