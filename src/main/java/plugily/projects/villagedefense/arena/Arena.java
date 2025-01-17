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

package plugily.projects.villagedefense.arena;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import plugily.projects.commonsbox.minecraft.compat.ServerVersion;
import plugily.projects.commonsbox.minecraft.configuration.ConfigUtils;
import plugily.projects.villagedefense.ConfigPreferences;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.api.event.game.VillageGameStateChangeEvent;
import plugily.projects.villagedefense.arena.managers.EnemySpawnManager;
import plugily.projects.villagedefense.arena.managers.ScoreboardManager;
import plugily.projects.villagedefense.arena.managers.ShopManager;
import plugily.projects.villagedefense.arena.managers.maprestorer.MapRestorerManager;
import plugily.projects.villagedefense.arena.managers.maprestorer.MapRestorerManagerLegacy;
import plugily.projects.villagedefense.arena.options.ArenaOption;
import plugily.projects.villagedefense.arena.states.ArenaStateHandler;
import plugily.projects.villagedefense.arena.states.EndingState;
import plugily.projects.villagedefense.arena.states.InGameState;
import plugily.projects.villagedefense.arena.states.RestartingState;
import plugily.projects.villagedefense.arena.states.StartingState;
import plugily.projects.villagedefense.arena.states.WaitingState;
import plugily.projects.villagedefense.creatures.CreatureUtils;
import plugily.projects.villagedefense.handlers.language.Messages;
import plugily.projects.villagedefense.user.User;
import plugily.projects.villagedefense.utils.Debugger;

/**
 * Created by Tom on 12/08/2014.
 */
public class Arena extends BukkitRunnable {

  private static Main plugin;
  private final String id;

  private final Set<Player> players = new HashSet<>();
  private final List<Creature> enemies = new ArrayList<>();
  private final List<Wolf> wolves = new ArrayList<>();
  private final List<Villager> villagers = new ArrayList<>();
  private final List<IronGolem> ironGolems = new ArrayList<>();
  private final List<Item> droppedFleshes = new ArrayList<>();

  //all arena values that are integers, contains constant and floating values
  private final Map<ArenaOption, Integer> arenaOptions = new EnumMap<>(ArenaOption.class);
  //instead of 3 location fields we use map with GameLocation enum
  private final Map<GameLocation, Location> gameLocations = new EnumMap<>(GameLocation.class);
  private final Map<SpawnPoint, List<Location>> spawnPoints = new EnumMap<>(SpawnPoint.class);
  //all handlers for all game states, we don't include them all in one runnable because it would be too big
  private final Map<ArenaState, ArenaStateHandler> gameStateHandlers = new EnumMap<>(ArenaState.class);

  private ScoreboardManager scoreboardManager;
  private MapRestorerManager mapRestorerManager;
  private ShopManager shopManager;
  private EnemySpawnManager enemySpawnManager;

  private ArenaState arenaState = ArenaState.WAITING_FOR_PLAYERS;
  private BossBar gameBar;
  private String mapName = "";
  private boolean fighting = false;
  private boolean forceStart = false;
  private boolean ready = true;

  @TestOnly
  protected Arena(String id, String mapName) {
    this.id = id;
    this.mapName = mapName;
    gameStateHandlers.put(ArenaState.WAITING_FOR_PLAYERS, new WaitingState());
    gameStateHandlers.put(ArenaState.STARTING, new StartingState());
    gameStateHandlers.put(ArenaState.IN_GAME, new InGameState());
    gameStateHandlers.put(ArenaState.ENDING, new EndingState());
    gameStateHandlers.put(ArenaState.RESTARTING, new RestartingState());
    for(ArenaStateHandler handler : gameStateHandlers.values()) {
      handler.init(plugin);
    }
  }

  public Arena(String id) {
    this.id = id == null ? "" : id;
    if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_9_R1) && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
      gameBar = Bukkit.createBossBar(plugin.getChatManager().colorMessage(Messages.BOSSBAR_MAIN_TITLE), BarColor.BLUE, BarStyle.SOLID);
    }
    shopManager = new ShopManager(this);
    enemySpawnManager = new EnemySpawnManager(this);
    scoreboardManager = new ScoreboardManager(this);
    if(ServerVersion.Version.isCurrentEqualOrLower(ServerVersion.Version.v1_16_R1)) {
      mapRestorerManager = new MapRestorerManagerLegacy(this);
    } else
      mapRestorerManager = new MapRestorerManager(this);
    setDefaultValues();
    gameStateHandlers.put(ArenaState.WAITING_FOR_PLAYERS, new WaitingState());
    gameStateHandlers.put(ArenaState.STARTING, new StartingState());
    gameStateHandlers.put(ArenaState.IN_GAME, new InGameState());
    gameStateHandlers.put(ArenaState.ENDING, new EndingState());
    gameStateHandlers.put(ArenaState.RESTARTING, new RestartingState());
    for(ArenaStateHandler handler : gameStateHandlers.values()) {
      handler.init(plugin);
    }
  }

  public static void init(Main plugin) {
    Arena.plugin = plugin;
  }

  private void setDefaultValues() {
    for(ArenaOption option : ArenaOption.values()) {
      arenaOptions.put(option, option.getDefaultValue());
    }
    for(GameLocation location : GameLocation.values()) {
      gameLocations.put(location, Bukkit.getWorlds().get(0).getSpawnLocation());
    }
    for(SpawnPoint point : SpawnPoint.values()) {
      spawnPoints.put(point, new ArrayList<>());
    }
  }

  public boolean isReady() {
    return ready;
  }

  public void setReady(boolean ready) {
    this.ready = ready;
  }

  public ShopManager getShopManager() {
    return shopManager;
  }

  public EnemySpawnManager getEnemySpawnManager() {
    return enemySpawnManager;
  }

  /**
   * Executes boss bar action for arena
   *
   * @param action add or remove a player from boss bar
   * @param p      player
   */
  public void doBarAction(BarAction action, Player p) {
    if(!ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_9_R1)
        || !plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
      return;
    }
    switch(action) {
      case ADD:
        gameBar.addPlayer(p);
        break;
      case REMOVE:
        gameBar.removePlayer(p);
        break;
      default:
        break;
    }
  }

  @Override
  public void run() {
    //idle task
    if(arenaState == ArenaState.WAITING_FOR_PLAYERS && players.isEmpty()) {
      return;
    }
    Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Running game task", id);
    long start = System.currentTimeMillis();

    gameStateHandlers.get(arenaState).handleCall(this);
    Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Game task finished took {1}ms", id, System.currentTimeMillis() - start);
  }

  public void spawnVillagers() {
    List<Location> villagerSpawns = getVillagerSpawns();
    if(villagerSpawns.isEmpty()) {
      Debugger.debug(Level.WARNING, "No villager spawns set for {0} game won't start", id);
      return;
    }

    int amount = plugin.getConfig().getInt("Villager-Amount", 10);
    int spawnSize = villagerSpawns.size();
    for (int i = 0; i < amount; i++) {
      spawnVillager(villagerSpawns.get(i % spawnSize));
    }

    if(villagers.isEmpty()) {
      Debugger.debug(Level.WARNING, "Spawning villagers for {0} failed! Are villager spawns set in safe and valid locations?", id);
    }
  }

  public boolean isForceStart() {
    return forceStart;
  }

  public void setForceStart(boolean forceStart) {
    this.forceStart = forceStart;
  }

  public boolean isFighting() {
    return fighting;
  }

  public void setFighting(boolean fighting) {
    this.fighting = fighting;
  }

  /**
   * Returns boss bar of the game.
   * Please use doBarAction if possible
   *
   * @return game boss bar
   * @see Arena#doBarAction(BarAction, Player)
   */
  public BossBar getGameBar() {
    return gameBar;
  }

  /**
   * Get arena identifier used to get arenas by string.
   *
   * @return arena name
   * @see ArenaRegistry#getArena(String)
   */
  public String getId() {
    return id;
  }

  public MapRestorerManager getMapRestorerManager() {
    return mapRestorerManager;
  }

  public int getMinimumPlayers() {
    return getOption(ArenaOption.MINIMUM_PLAYERS);
  }

  public void setMinimumPlayers(int minimumPlayers) {
    setOptionValue(ArenaOption.MINIMUM_PLAYERS, minimumPlayers);
  }

  /**
   * Get arena map name.
   *
   * @return arena map name, <b>it's not arena id</b>
   * @see #getId()
   */
  public String getMapName() {
    return mapName;
  }

  /**
   * Set arena map name.
   *
   * @param mapName new map name, [b]it's not arena id[/b]
   */
  public void setMapName(String mapName) {
    this.mapName = mapName;
  }

  /**
   * Get timer of arena.
   *
   * @return timer of lobby time / time to next wave
   */
  public int getTimer() {
    return getOption(ArenaOption.TIMER);
  }

  /**
   * Modify game timer.
   *
   * @param timer timer of lobby / time to next wave
   */
  public void setTimer(int timer) {
    setOptionValue(ArenaOption.TIMER, timer);
  }

  public int getMaximumPlayers() {
    return getOption(ArenaOption.MAXIMUM_PLAYERS);
  }

  public void setMaximumPlayers(int maximumPlayers) {
    setOptionValue(ArenaOption.MAXIMUM_PLAYERS, maximumPlayers);
  }

  @NotNull
  public ArenaState getArenaState() {
    return arenaState;
  }

  /**
   * Set game state of arena.
   * Calls VillageGameStateChangeEvent
   *
   * @param arenaState new game state of arena
   * @see ArenaState
   * @see VillageGameStateChangeEvent
   */
  public void setArenaState(@NotNull ArenaState arenaState) {
    this.arenaState = arenaState;
    Bukkit.getPluginManager().callEvent(new VillageGameStateChangeEvent(this, arenaState));
    plugin.getSignManager().updateSigns();
  }

  @NotNull
  public Set<Player> getPlayers() {
    return players;
  }

  public Location getLobbyLocation() {
    return gameLocations.get(GameLocation.LOBBY);
  }

  public void setLobbyLocation(Location loc) {
    gameLocations.put(GameLocation.LOBBY, loc);
  }

  public Location getStartLocation() {
    return gameLocations.get(GameLocation.START);
  }

  public void setStartLocation(Location location) {
    gameLocations.put(GameLocation.START, location);
  }

  public void teleportToEndLocation(Player player) {
    // We should check for #isEnabled to make sure plugin is enabled
    // This happens in some cases
    if(plugin.isEnabled() && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)
        && ConfigUtils.getConfig(plugin, "bungee").getBoolean("End-Location-Hub", true)) {
      plugin.getBungeeManager().connectToHub(player);
      Debugger.debug("{0} has left the arena {1}! Teleported to the Hub server.", player.getName(), this);
    }

    player.teleport(getEndLocation());
  }

  public Location getEndLocation() {
    return gameLocations.get(GameLocation.END);
  }

  public void setEndLocation(Location endLoc) {
    gameLocations.put(GameLocation.END, endLoc);
  }

  public void start() {
    Debugger.debug("[{0}] Instance started", id);
    runTaskTimer(plugin, 20L, 20L);
    setArenaState(ArenaState.WAITING_FOR_PLAYERS);
  }

  public ScoreboardManager getScoreboardManager() {
    return scoreboardManager;
  }

  /**
   * Get list of already spawned enemies.
   * This will only return alive enemies not total enemies in current wave.
   *
   * @return list of spawned enemies in arena
   */
  @NotNull
  public List<Creature> getEnemies() {
    return enemies;
  }

  public void removeEnemy(Creature enemy) {
    enemies.remove(enemy);
  }

  @NotNull
  public List<Location> getVillagerSpawns() {
    return spawnPoints.getOrDefault(SpawnPoint.VILLAGER, new ArrayList<>());
  }

  public void addVillagerSpawn(Location location) {
    getVillagerSpawns().add(location);
  }

  public void addZombieSpawn(Location location) {
    getZombieSpawns().add(location);
  }

  @NotNull
  public List<Item> getDroppedFleshes() {
    return droppedFleshes;
  }

  public void addDroppedFlesh(Item item) {
    droppedFleshes.add(item);
  }

  public void removeDroppedFlesh(Item item) {
    droppedFleshes.remove(item);
  }

  public int getZombiesLeft() {
    return getOption(ArenaOption.ZOMBIES_TO_SPAWN) + enemies.size();
  }

  public int getWave() {
    return getOption(ArenaOption.WAVE);
  }

  /**
   * Should be used with endWave.
   *
   * @param wave new game wave
   * @see ArenaManager#endWave(Arena)
   */
  public void setWave(int wave) {
    setOptionValue(ArenaOption.WAVE, wave);
  }

  public void spawnVillager(Location location) {
    Villager villager = CreatureUtils.getCreatureInitializer().spawnVillager(location);
    villager.setCustomNameVisible(plugin.getConfigPreferences().getOption(ConfigPreferences.Option.NAME_VISIBLE_VILLAGER));
    villager.setCustomName(CreatureUtils.getRandomVillagerName());
    addVillager(villager);
  }

  public void spawnWolf(Location location, Player player) {
    if(!canSpawnMobForPlayer(player, EntityType.WOLF)) {
      return;
    }

    Wolf wolf = CreatureUtils.getCreatureInitializer().spawnWolf(location);
    wolf.setOwner(player);
    wolf.setCustomNameVisible(plugin.getConfigPreferences().getOption(ConfigPreferences.Option.NAME_VISIBLE_WOLF));
    wolf.setCustomName(plugin.getChatManager().colorMessage(Messages.SPAWNED_WOLF_NAME).replace("%player%", player.getName()));
    addWolf(wolf);
  }

  public void spawnGolem(Location location, Player player) {
    if(!canSpawnMobForPlayer(player, EntityType.IRON_GOLEM)) {
      return;
    }

    IronGolem ironGolem = CreatureUtils.getCreatureInitializer().spawnGolem(location);
    ironGolem.setCustomNameVisible(plugin.getConfigPreferences().getOption(ConfigPreferences.Option.NAME_VISIBLE_GOLEM));
    ironGolem.setCustomName(plugin.getChatManager().colorMessage(Messages.SPAWNED_GOLEM_NAME).replace("%player%", player.getName()));
    addIronGolem(ironGolem);
  }

  protected void addWolf(Wolf wolf) {
    wolves.add(wolf);
  }

  protected boolean canSpawnMobForPlayer(Player player, EntityType type) {
    if(type != EntityType.IRON_GOLEM && type != EntityType.WOLF) {
      return true;
    }

    for(Map.Entry<String, Boolean> map : getAllEffectivePermissions(player).entrySet()) {
      if(!map.getValue()) {
        continue;
      }

      int limit = 0;
      try {
        limit = Integer.parseInt(map.getKey().split("\\.", 2)[1]);
      } catch(NumberFormatException ex) {
      }

      if(limit < 1) {
        continue;
      }

      if((type == EntityType.IRON_GOLEM && map.getKey().endsWith("limit.golem." + limit) && ironGolems.size() + 1 < limit)
          || (type == EntityType.WOLF && map.getKey().endsWith("limit.wolf." + limit) && wolves.size() + 1 < limit)) {
        return false;
      }
    }

    return true;
  }

  private Map<String, Boolean> getAllEffectivePermissions(Player player) {
    Map<String, Boolean> permLimits = new HashMap<>();
    for(PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
      if(permission.getPermission().startsWith("villagedefense.limit.")) {
        permLimits.put(permission.getPermission(), permission.getValue());
      }
    }

    return permLimits;
  }

  /**
   * Get alive wolves.
   *
   * @return alive wolves in game
   */
  @NotNull
  public List<Wolf> getWolves() {
    return wolves;
  }

  /**
   * Get alive iron golems.
   *
   * @return alive iron golems in game
   */
  @NotNull
  public List<IronGolem> getIronGolems() {
    return ironGolems;
  }

  /**
   * Get alive villagers.
   *
   * @return alive villagers in game
   */
  @NotNull
  public List<Villager> getVillagers() {
    return villagers;
  }

  public boolean checkLevelUpRottenFlesh() {
    int rottenFleshLevel = getOption(ArenaOption.ROTTEN_FLESH_LEVEL);
    int rottenFleshAmount = getOption(ArenaOption.ROTTEN_FLESH_AMOUNT);

    if(rottenFleshLevel == 0 && rottenFleshAmount > 50) {
      setOptionValue(ArenaOption.ROTTEN_FLESH_LEVEL, 1);
      return true;
    }

    if(rottenFleshLevel * 10 * players.size() + 10 < rottenFleshAmount) {
      addOptionValue(ArenaOption.ROTTEN_FLESH_LEVEL, 1);
      return true;
    }

    return false;
  }

  @NotNull
  public List<Player> getPlayersLeft() {
    List<Player> list = new ArrayList<>();

    for (Player player : players) {
      User user = plugin.getUserManager().getUser(player);
      if (!user.isSpectator()) {
        list.add(user.getPlayer());
      }
    }

    return list;
  }

  public Main getPlugin() {
    return plugin;
  }

  protected void addVillager(Villager villager) {
    villagers.add(villager);
  }

  public void removeVillager(Villager villager) {
    villager.remove();
    villager.setHealth(0);
    villagers.remove(villager);
  }

  @NotNull
  public List<Location> getZombieSpawns() {
    return spawnPoints.getOrDefault(SpawnPoint.ZOMBIE, new ArrayList<>());
  }

  public final Location getRandomZombieSpawn(Random random) {
    List<Location> spawns = getZombieSpawns();
    return spawns.get(spawns.size() == 1 ? 0 : random.nextInt(spawns.size()));
  }

  protected void addIronGolem(IronGolem ironGolem) {
    ironGolems.add(ironGolem);
  }

  public void removeIronGolem(IronGolem ironGolem) {
    ironGolem.remove();
    ironGolems.remove(ironGolem);
  }

  public void removeWolf(Wolf wolf) {
    wolf.remove();
    wolves.remove(wolf);
  }

  public void resetOptionValues() {
    setOptionValue(ArenaOption.WAVE, 1);
    setOptionValue(ArenaOption.TOTAL_KILLED_ZOMBIES, 0);
    setOptionValue(ArenaOption.TOTAL_ORBS_SPENT, 0);
    setOptionValue(ArenaOption.ZOMBIE_DIFFICULTY_MULTIPLIER, 1);
    setOptionValue(ArenaOption.ZOMBIE_IDLE_PROCESS, 0);
    enemySpawnManager.applyIdle(0);
  }

  public int getOption(ArenaOption option) {
    return arenaOptions.getOrDefault(option, 0);
  }

  public void setOptionValue(ArenaOption option, int value) {
    arenaOptions.put(option, value);
  }

  public void addOptionValue(ArenaOption option, int value) {
    arenaOptions.put(option, getOption(option) + value);
  }

  public enum BarAction {
    ADD, REMOVE
  }

  public enum GameLocation {
    START, LOBBY, END
  }

  public enum SpawnPoint {
    ZOMBIE, VILLAGER
  }

}