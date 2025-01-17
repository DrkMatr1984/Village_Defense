/*
 * Village Defense - Protect villagers from hordes of zombies
 * Copyright (C) 2021  Plugily Projects - maintained by 2Wild4You, Tigerpanzer_02 and contributors
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

package plugily.projects.villagedefense.arena.states;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.api.StatsStorage;
import plugily.projects.villagedefense.api.event.game.VillageGameStartEvent;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.arena.ArenaState;
import plugily.projects.villagedefense.handlers.language.Messages;
import plugily.projects.villagedefense.user.User;

/**
 * @author Plajer
 * <p>
 * Created at 03.06.2019
 */
public class StartingState implements ArenaStateHandler {

  private Main plugin;

  @Override
  public void init(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public void handleCall(Arena arena) {
    double startWaiting = plugin.getConfig().getDouble("Starting-Waiting-Time", 60);
    int timer = arena.getTimer();

    if(arena.getGameBar() != null) {
      arena.getGameBar().setTitle(plugin.getChatManager().colorMessage(Messages.BOSSBAR_STARTING_IN).replace("%time%", Integer.toString(timer)));
      arena.getGameBar().setProgress(timer / startWaiting);
    }
    for(Player player : arena.getPlayers()) {
      player.setExp((float) (timer / startWaiting));
      player.setLevel(timer);
    }

    int minPlayers = arena.getMinimumPlayers();

    if(!arena.isForceStart() && arena.getPlayers().size() < minPlayers) {
      if(arena.getGameBar() != null) {
        arena.getGameBar().setTitle(plugin.getChatManager().colorMessage(Messages.BOSSBAR_WAITING_FOR_PLAYERS));
        arena.getGameBar().setProgress(1.0);
      }
      plugin.getChatManager().broadcastMessage(arena, plugin.getChatManager().formatMessage(arena, plugin.getChatManager().colorMessage(Messages.LOBBY_MESSAGES_WAITING_FOR_PLAYERS), minPlayers));
      arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
      Bukkit.getPluginManager().callEvent(new VillageGameStartEvent(arena));
      arena.setTimer(15);
      for(Player player : arena.getPlayers()) {
        player.setExp(1);
        player.setLevel(0);
      }
      return;
    }
    if(arena.getTimer() == 0 || arena.isForceStart()) {
      arena.spawnVillagers();
      Bukkit.getPluginManager().callEvent(new VillageGameStartEvent(arena));
      arena.setArenaState(ArenaState.IN_GAME);
      if(arena.getGameBar() != null) {
        arena.getGameBar().setProgress(1.0);
      }
      arena.setTimer(5);

      org.bukkit.Location arenaLoc = arena.getStartLocation();
      int orbsStartingAmount = plugin.getConfig().getInt("Orbs-Starting-Amount", 20);

      for(Player player : arena.getPlayers()) {
        player.teleport(arenaLoc);
        player.setExp(0);
        player.setLevel(0);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        User user = plugin.getUserManager().getUser(player);
        user.setStat(StatsStorage.StatisticType.ORBS, orbsStartingAmount);
        user.getKit().giveKitItems(player);
        player.updateInventory();
        plugin.getUserManager().addExperience(player, 10);
        player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage(Messages.LOBBY_MESSAGES_GAME_STARTED));
      }
      arena.setTimer(plugin.getConfig().getInt("Cooldown-Before-Next-Wave", 25));
      arena.setFighting(false);
    }
    if(arena.isForceStart()) {
      arena.setForceStart(false);
    }
    arena.setTimer(arena.getTimer() - 1);
  }

}
