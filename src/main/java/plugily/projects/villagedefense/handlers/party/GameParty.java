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

package plugily.projects.villagedefense.handlers.party;

import java.util.List;
import org.bukkit.entity.Player;

/**
 * @author Plajer
 * <p>
 * Created at 09.02.2020
 */
public class GameParty {

  private final List<Player> players;
  private final Player leader;

  public GameParty(List<Player> players, Player leader) {
    this.players = players;
    this.leader = leader;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public Player getLeader() {
    return leader;
  }
}
