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

package plugily.projects.villagedefense.handlers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import plugily.projects.villagedefense.api.StatsStorage;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.arena.ArenaRegistry;

/**
 * @author Plajer
 * <p>
 * Created at 05.05.2018
 */
public class PlaceholderManager extends PlaceholderExpansion {

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "villagedefense";
  }

  @Override
  public @NotNull String getAuthor() {
    return "Plugily Projects";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0.2";
  }

  @Override
  public String onPlaceholderRequest(Player player, String id) {
    if(player == null) {
      return null;
    }
    switch(id.toLowerCase()) {
      case "kills":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.KILLS));
      case "deaths":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.DEATHS));
      case "games_played":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.GAMES_PLAYED));
      case "highest_wave":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.HIGHEST_WAVE));
      case "level":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LEVEL));
      case "exp":
        return Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.XP));
      case "exp_to_next_level":
        return Double.toString(Math.ceil(Math.pow(50 * StatsStorage.getUserStats(player, StatsStorage.StatisticType.LEVEL), 1.5)));
      case "arena_players_online":
        return Integer.toString(ArenaRegistry.getArenaPlayersOnline());
      default:
        return handleArenaPlaceholderRequest(id);
    }
  }

  private String handleArenaPlaceholderRequest(String id) {
    String[] data = id.split(":", 2);
    if (data.length < 2) {
      return null;
    }

    Arena arena = ArenaRegistry.getArena(data[0]);
    if(arena == null) {
      return null;
    }
    switch(data[1].toLowerCase()) {
      case "players":
        return Integer.toString(arena.getPlayers().size());
      case "max_players":
        return Integer.toString(arena.getMaximumPlayers());
      case "state":
        return arena.getArenaState().toString().toLowerCase();
      case "state_pretty":
        return arena.getArenaState().getPlaceholder();
      case "wave":
        return Integer.toString(arena.getWave());
      case "mapname":
        return arena.getMapName();
      default:
        return null;
    }
  }

}
