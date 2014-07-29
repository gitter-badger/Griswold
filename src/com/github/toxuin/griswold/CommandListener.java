package com.github.toxuin.griswold;

import com.github.toxuin.griswold.npcs.GriswoldNPC;
import com.github.toxuin.griswold.professions.Profession;
import com.github.toxuin.griswold.util.ConfigManager;
import com.github.toxuin.griswold.util.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandListener implements CommandExecutor {
    private Griswold plugin;

    public CommandListener(Griswold griswold) {
        this.plugin = griswold;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) { sender.sendMessage(Lang.error_few_arguments); return false; }
        if (!can(sender, args[0])) { sender.sendMessage(ChatColor.RED+Lang.error_accesslevel); return true; }

        // RELOAD COMMAND
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            return true;
        }

        // CREATE COMMAND
        else if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                sender.sendMessage(Lang.insufficient_params);
                return true;
            }

            Player player = (Player) sender;
            Location location = player.getLocation().toVector().add(player.getLocation().getDirection().multiply(3)).toLocation(player.getWorld());
            location.setY(Math.round(player.getLocation().getY()));

            if (Griswold.plugin.getNpcByName(args[1]) != null) {
                player.sendMessage(String.format(Lang.repairman_exists, args[1]));
                return true;
            }

            Profession profession = Profession.getByName(args[2]);
            if (profession == null) {
                player.sendMessage(Lang.error_profession_not_found + Profession.knownProfessions.keySet());
                return true;
            }

            // TODO: COUNT THE PARAMETERS!
            plugin.createNpc(args[1], location, profession);
            player.sendMessage(Lang.new_created);
            return true;
        }

        // REMOVE COMMAND
        else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                sender.sendMessage(Lang.error_few_arguments);
                return true;
            }

            ConfigManager.removeNpc(args[1]);
            sender.sendMessage(String.format(Lang.deleted, args[1]));
            return true;
        }

        // LIST COMMAND
        else if (args[0].equalsIgnoreCase("list")) {
            plugin.listNpc(sender);
            return true;
        }

        // DESPAWN
        else if (args[0].equalsIgnoreCase("despawn")) {
            plugin.despawnAll();
            sender.sendMessage(Lang.despawned);
            return true;
        }

        // NAMES COMMAND
        else if (args[0].equalsIgnoreCase("name")) {
            if (args.length < 2) {
                sender.sendMessage(Lang.error_few_arguments);
                return true;
            }
            GriswoldNPC npc = plugin.getNpcByName(args[1]);
            boolean isNameVisible = npc.isNameVisible();
            npc.setNameVisible(!isNameVisible);
            sender.sendMessage(!isNameVisible ?Lang.names_on:Lang.names_off);
            return true;
        }

        // SOUND COMMAND
        else if (args[0].equalsIgnoreCase("sound")) {
            if (args.length < 3) {
                sender.sendMessage(Lang.error_few_arguments);
                return true;
            }

            GriswoldNPC npc = plugin.getNpcByName(args[1]);
            if (npc != null) {
                npc.setSound(args[2]);
                sender.sendMessage(String.format(Lang.sound_changed, args[1]));
            } else {
                sender.sendMessage(String.format(Lang.error_npc_not_found, args[1]));
            }
        }

        return false;
    }

	private boolean can(CommandSender sender, String command) {
		if (!(command.equalsIgnoreCase("reload") || command.equalsIgnoreCase("create") ||
				command.equalsIgnoreCase("remove") || command.equalsIgnoreCase("list") ||
				command.equalsIgnoreCase("despawn") || command.equalsIgnoreCase("names") ||
				command.equalsIgnoreCase("sound"))) {
			// UNKNOWN COMMAND.
			return true;
		}

		if (!command.equalsIgnoreCase("create") && !(sender instanceof Player)) {
			return false;
		}

        return sender instanceof ConsoleCommandSender || sender.hasPermission("griswold.admin");
    }
}
