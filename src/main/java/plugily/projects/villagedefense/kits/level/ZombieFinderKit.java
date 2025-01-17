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

package plugily.projects.villagedefense.kits.level;

import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.commonsbox.minecraft.compat.events.api.CBPlayerInteractEvent;
import plugily.projects.commonsbox.minecraft.compat.xseries.XMaterial;
import plugily.projects.commonsbox.minecraft.helper.WeaponHelper;
import plugily.projects.commonsbox.minecraft.item.ItemBuilder;
import plugily.projects.commonsbox.minecraft.item.ItemUtils;
import plugily.projects.commonsbox.minecraft.misc.stuff.ComplementAccessor;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.arena.ArenaRegistry;
import plugily.projects.villagedefense.handlers.language.Messages;
import plugily.projects.villagedefense.kits.KitRegistry;
import plugily.projects.villagedefense.kits.basekits.LevelKit;
import plugily.projects.villagedefense.user.User;
import plugily.projects.villagedefense.utils.Utils;

/**
 * Created by Tom on 21/07/2015.
 */
public class ZombieFinderKit extends LevelKit implements Listener {

  public ZombieFinderKit() {
    setName(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_NAME));
    List<String> description = Utils.splitString(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_DESCRIPTION), 40);
    setDescription(description.toArray(new String[0]));
    setLevel(getKitsConfig().getInt("Required-Level.ZombieFinder"));
    getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    KitRegistry.registerKit(this);
  }

  @Override
  public boolean isUnlockedByPlayer(Player player) {
    return true;
  }

  @Override
  public void giveKitItems(Player player) {
    player.getInventory().addItem(WeaponHelper.getUnBreakingSword(WeaponHelper.ResourceType.WOOD, 10));
    player.getInventory().addItem(new ItemStack(XMaterial.COOKED_PORKCHOP.parseMaterial(), 8));
    player.getInventory().addItem(new ItemBuilder(WeaponHelper.getEnchanted(new ItemStack(Material.BOOK), new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{1}))
        .name(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_GAME_ITEM_NAME))
        .lore(Utils.splitString(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_GAME_ITEM_LORE), 40))
        .build());
  }

  @Override
  public Material getMaterial() {
    return Material.FISHING_ROD;
  }

  @Override
  public void reStock(Player player) {
    //no restock items for this kit
  }

  @EventHandler
  public void onTeleport(CBPlayerInteractEvent event) {
    Arena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null || !ItemUtils.isItemStackNamed(event.getItem()) || event.getItem().getType() != Material.BOOK
        || !ComplementAccessor.getComplement().getDisplayName(event.getItem().getItemMeta()).equals(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_GAME_ITEM_NAME))) {
      return;
    }
    User user = getPlugin().getUserManager().getUser(event.getPlayer());
    if(user.isSpectator()) {
      event.getPlayer().sendMessage(getPlugin().getChatManager().colorMessage(Messages.SPECTATOR_WARNING));
      return;
    }
    if (!(user.getKit() instanceof ZombieFinderKit)) {
      return;
    }
    long zombieCooldown = user.getCooldown("zombie");
    if(zombieCooldown > 0 && !user.isSpectator()) {
      String message = getPlugin().getChatManager().colorMessage(Messages.KITS_ABILITY_STILL_ON_COOLDOWN);
      message = message.replaceFirst("%COOLDOWN%", Long.toString(zombieCooldown));
      event.getPlayer().sendMessage(message);
      return;
    }
    if(arena.getEnemies().isEmpty()) {
      event.getPlayer().sendMessage(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_NO_AVAILABLE_ZOMBIES));
      return;
    }

    Creature creature = arena.getEnemies().get(arena.getEnemies().size() == 1 ? 0 : new Random().nextInt(arena.getEnemies().size()));
    creature.teleport(event.getPlayer());
    creature.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 30, 0));
    event.getPlayer().sendMessage(getPlugin().getChatManager().colorMessage(Messages.KITS_ZOMBIE_TELEPORTER_ZOMBIE_TELEPORTED));
    Utils.playSound(event.getPlayer().getLocation(), "ENTITY_ZOMBIE_DEATH", "ENTITY_ZOMBIE_DEATH");
    user.setCooldown("zombie", getKitsConfig().getInt("Kit-Cooldown.Zombie-Finder", 30));
  }
}
