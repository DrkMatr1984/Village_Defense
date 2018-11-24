/*
 * Village Defense 4 - Protect villagers from hordes of zombies
 * Copyright (C) 2018  Plajer's Lair - maintained by Plajer and Tigerpanzer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.plajer.villagedefense.commands.arguments.admin.arena;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import pl.plajer.villagedefense.arena.Arena;
import pl.plajer.villagedefense.arena.ArenaManager;
import pl.plajer.villagedefense.arena.ArenaRegistry;
import pl.plajer.villagedefense.commands.arguments.ArgumentsRegistry;
import pl.plajer.villagedefense.commands.arguments.CommandArgument;
import pl.plajer.villagedefense.handlers.ChatManager;
import pl.plajerlair.core.utils.ConfigUtils;

/**
 * @author Plajer
 * <p>
 * Created at 24.11.2018
 */
public class DeleteArgument {

  public DeleteArgument(ArgumentsRegistry registry) {
    registry.mapArgument("villagedefenseadmin", new CommandArgument("delete", "villagedefense.admin.delete", CommandArgument.ExecutorType.PLAYER) {
      @Override
      public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
          sender.sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Commands.Type-Arena-Name"));
          return;
        }
        Arena arena = ArenaRegistry.getArena(args[1]);
        if (arena == null) {
          sender.sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Commands.No-Arena-Like-That"));
          return;
        }
        ArenaManager.stopGame(false, arena);
        FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
        config.set("instances." + args[1], null);
        ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
        ArenaRegistry.unregisterArena(arena);
        sender.sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Commands.Removed-Game-Instance"));
      }
    });
  }

}
