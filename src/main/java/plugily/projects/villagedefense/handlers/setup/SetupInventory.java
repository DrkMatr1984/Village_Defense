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

package plugily.projects.villagedefense.handlers.setup;

import java.util.Random;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import plugily.projects.commonsbox.minecraft.configuration.ConfigUtils;
import plugily.projects.inventoryframework.gui.type.ChestGui;
import plugily.projects.inventoryframework.pane.StaticPane;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.handlers.setup.components.ArenaRegisterComponent;
import plugily.projects.villagedefense.handlers.setup.components.MiscComponents;
import plugily.projects.villagedefense.handlers.setup.components.PlayerAmountComponents;
import plugily.projects.villagedefense.handlers.setup.components.SpawnComponents;
import plugily.projects.villagedefense.utils.constants.Constants;

/**
 * Created by Tom on 15/06/2015.
 */
public class SetupInventory {

  public static final String VIDEO_LINK = "https://tutorial.plugily.xyz";
  private static final Random random = new Random();
  private static Main plugin;
  private final FileConfiguration config;
  private final Arena arena;
  private final Player player;
  private ChestGui gui;
  private final SetupUtilities setupUtilities;

  public SetupInventory(Arena arena, Player player) {
    config = ConfigUtils.getConfig(plugin, Constants.Files.ARENAS.getName());
    this.arena = arena;
    this.player = player;
    setupUtilities = new SetupUtilities(config, arena);
    prepareGui();
  }

  public static void init(Main plugin) {
    SetupInventory.plugin = plugin;
  }

  private void prepareGui() {
    gui = new ChestGui(2, "Village Defense Arena Setup");
    gui.setOnGlobalClick(e -> e.setCancelled(true));
    StaticPane pane = new StaticPane(9, 4);
    gui.addPane(pane);

    prepareComponents(pane);
  }

  private void prepareComponents(StaticPane pane) {
    SpawnComponents spawnComponents = new SpawnComponents();
    spawnComponents.prepare(this);
    spawnComponents.injectComponents(pane);

    PlayerAmountComponents playerAmountComponents = new PlayerAmountComponents();
    playerAmountComponents.prepare(this);
    playerAmountComponents.injectComponents(pane);

    MiscComponents miscComponents = new MiscComponents();
    miscComponents.prepare(this);
    miscComponents.injectComponents(pane);

    ArenaRegisterComponent arenaRegisterComponent = new ArenaRegisterComponent();
    arenaRegisterComponent.prepare(this);
    arenaRegisterComponent.injectComponents(pane);
  }

  private void sendProTip(Player p) {
    switch(random.nextInt(8 + 1)) {
      case 0:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Console can execute /vda addorbs [amount] (player) command! Add game orbs via console!"));
        break;
      case 1:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Build Secret Well for your arena! Check how: https://wiki.plugily.xyz/villagedefense/setup/simple#how-can-i-set-up-secret-well"));
        break;
      case 2:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Help us translating plugin to your language here: https://translate.plugily.xyz"));
        break;
      case 3:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7PlaceholderApi plugin is supported with our plugin! Check here: https://wiki.plugily.xyz/villagedefense/placeholders/placeholderapi"));
        break;
      case 4:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Achievements, custom kits and replay ability are things available in our paid addon for this minigame!"));
        break;
      case 5:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7We are open source! You can always help us by contributing! Check https://github.com/Plugily-Projects/Village_Defense"));
        break;
      case 6:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Need help? Check wiki &8https://wiki.plugily.xyz/villagedefense &7or discord https://discord.gg/UXzUdTP"));
        break;
      case 7:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Install HolographicDisplays plugin to access power-ups in game! (configure them in config.yml)"));
        break;
      case 8:
        p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Suggest new ideas for the plugin or vote on current ones! https://app.feedbacky.net/b/VillageDefense"));
        break;
      default:
        break;
    }
  }

  public void openInventory() {
    sendProTip(player);
    gui.show(player);
  }

  public Main getPlugin() {
    return plugin;
  }

  public FileConfiguration getConfig() {
    return config;
  }

  public Arena getArena() {
    return arena;
  }

  public Player getPlayer() {
    return player;
  }

  public ChestGui getGui() {
    return gui;
  }

  public SetupUtilities getSetupUtilities() {
    return setupUtilities;
  }
}
