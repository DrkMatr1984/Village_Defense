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

package plugily.projects.villagedefense.kits.premium;

import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import plugily.projects.commonsbox.minecraft.compat.VersionUtils;
import plugily.projects.commonsbox.minecraft.compat.events.api.CBPlayerInteractEvent;
import plugily.projects.commonsbox.minecraft.compat.xseries.XMaterial;
import plugily.projects.commonsbox.minecraft.helper.ArmorHelper;
import plugily.projects.commonsbox.minecraft.helper.WeaponHelper;
import plugily.projects.commonsbox.minecraft.item.ItemBuilder;
import plugily.projects.commonsbox.minecraft.item.ItemUtils;
import plugily.projects.commonsbox.minecraft.misc.stuff.ComplementAccessor;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.arena.ArenaRegistry;
import plugily.projects.villagedefense.handlers.PermissionsManager;
import plugily.projects.villagedefense.handlers.language.Messages;
import plugily.projects.villagedefense.kits.KitRegistry;
import plugily.projects.villagedefense.kits.basekits.PremiumKit;
import plugily.projects.villagedefense.utils.Utils;

/**
 * Created by Tom on 17/12/2015.
 */
public class BlockerKit extends PremiumKit implements Listener {

  public BlockerKit() {
    setName(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_NAME));
    List<String> description = Utils.splitString(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_DESCRIPTION), 40);
    setDescription(description.toArray(new String[0]));
    getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    KitRegistry.registerKit(this);
  }

  @Override
  public boolean isUnlockedByPlayer(Player player) {
    return PermissionsManager.isPremium(player) || player.hasPermission("villagedefense.kit.blocker");
  }

  @Override
  public void giveKitItems(Player player) {
    ArmorHelper.setColouredArmor(Color.RED, player);
    player.getInventory().addItem(WeaponHelper.getEnchanted(new ItemStack(Material.STONE_SWORD), new org.bukkit.enchantments.Enchantment[]{org.bukkit.enchantments.Enchantment.DURABILITY}, new int[]{10}));
    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 10));
    player.getInventory().addItem(new ItemBuilder(new ItemStack(XMaterial.OAK_FENCE.parseMaterial(), 3))
        .name(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_NAME))
        .lore(Utils.splitString(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_LORE), 40))
        .build());
    player.getInventory().addItem(new ItemStack(Material.SADDLE));

  }

  @Override
  public Material getMaterial() {
    return Material.BARRIER;
  }

  @Override
  public void reStock(Player player) {
    player.getInventory().addItem(new ItemBuilder(new ItemStack(XMaterial.OAK_FENCE.parseMaterial(), 3))
        .name(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_NAME))
        .lore(Utils.splitString(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_LORE), 40))
        .build());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBarrierPlace(CBPlayerInteractEvent event) {
    if(event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Player player = event.getPlayer();
    ItemStack stack = VersionUtils.getItemInHand(player);
    if(!ArenaRegistry.isInArena(player) || !ItemUtils.isItemStackNamed(stack)
        || !ComplementAccessor.getComplement().getDisplayName(stack.getItemMeta())
        .equalsIgnoreCase(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_NAME))) {
      return;
    }
    if (!(getPlugin().getUserManager().getUser(player).getKit() instanceof BlockerKit)) {
      return;
    }
    Block block = null;
    for(Block blocks : player.getLastTwoTargetBlocks(null, 5)) {
      if(blocks.getType() == Material.AIR) {
        block = blocks;
      }
    }
    if(block == null) {
      event.getPlayer().sendMessage(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_PLACE_FAIL));
      return;
    }
    Utils.takeOneItem(player, stack);
    event.setCancelled(false);

    event.getPlayer().sendMessage(getPlugin().getChatManager().colorMessage(Messages.KITS_BLOCKER_GAME_ITEM_PLACE_MESSAGE));
    ZombieBarrier zombieBarrier = new ZombieBarrier();
    zombieBarrier.setLocation(block.getLocation());
    Arena arena = ArenaRegistry.getArena(player);
    VersionUtils.sendParticles("FIREWORKS_SPARK", arena.getPlayers(), zombieBarrier.location, 20);
    removeBarrierLater(zombieBarrier, arena);
    block.setType(XMaterial.OAK_FENCE.parseMaterial());
  }

  private void removeBarrierLater(ZombieBarrier zombieBarrier, Arena arena) {
    new BukkitRunnable() {
      @Override
      public void run() {
        zombieBarrier.decrementSeconds();
        if(zombieBarrier.seconds <= 0) {
          zombieBarrier.location.getBlock().setType(Material.AIR);
          VersionUtils.sendParticles("FIREWORKS_SPARK", arena.getPlayers(), zombieBarrier.location, 20);
          cancel();
        }
      }
    }.runTaskTimer(getPlugin(), 20, 20);
  }

  private static class ZombieBarrier {
    private Location location;
    private int seconds = 10;

    void setLocation(Location location) {
      this.location = location;
    }

    void decrementSeconds() {
      seconds--;
    }
  }
}
