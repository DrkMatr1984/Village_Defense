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

package plugily.projects.villagedefense.creatures.v1_14_R1;

import net.minecraft.server.v1_14_R1.EntityHuman;
import net.minecraft.server.v1_14_R1.EntityInsentient;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EntityVillager;
import net.minecraft.server.v1_14_R1.EntityZombie;
import net.minecraft.server.v1_14_R1.Navigation;
import net.minecraft.server.v1_14_R1.PathfinderGoalAvoidTarget;
import net.minecraft.server.v1_14_R1.PathfinderGoalFloat;
import net.minecraft.server.v1_14_R1.PathfinderGoalInteract;
import net.minecraft.server.v1_14_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_14_R1.PathfinderGoalLookAtTradingPlayer;
import net.minecraft.server.v1_14_R1.PathfinderGoalMoveTowardsRestriction;
import net.minecraft.server.v1_14_R1.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_14_R1.PathfinderGoalTradeWithPlayer;
import net.minecraft.server.v1_14_R1.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;

/**
 * Created by Tom on 15/08/2014.
 */
public class RidableVillager extends EntityVillager {

  public RidableVillager(org.bukkit.World world) {
    this(((CraftWorld) world).getHandle());
  }

  public RidableVillager(World world) {
    super(EntityTypes.VILLAGER, world);

    GoalSelectorCleaner.clearSelectors(this);

    //todo this.setSize(0.6F, 1.8F);
    getNavigation().q().b(true);
    ((Navigation) getNavigation()).a(true);
    goalSelector.a(0, new PathfinderGoalFloat(this));
    goalSelector.a(1, new PathfinderGoalAvoidTarget<>(this, EntityZombie.class,
        8.0F, 0.6D, 0.6D));
    goalSelector.a(1, new PathfinderGoalTradeWithPlayer(this));
    goalSelector.a(1, new PathfinderGoalLookAtTradingPlayer(this));
    //todo
    //this.goalSelector.a(2, new PathfinderGoalMoveIndoors(this));
    //this.goalSelector.a(3, new PathfinderGoalRestrictOpenDoor(this));
    //this.goalSelector.a(4, new PathfinderGoalOpenDoor(this, true));
    goalSelector.a(5, new PathfinderGoalMoveTowardsRestriction(this, 0.6D));
    //this.goalSelector.a(6, new PathfinderGoalMakeLove(this));
    //this.goalSelector.a(8, new PathfinderGoalPlay(this, 0.32D));
    goalSelector.a(9, new PathfinderGoalInteract(this, EntityHuman.class,
        3.0F, 1.0F));
    goalSelector.a(9, new PathfinderGoalInteract(this, EntityVillager.class,
        5.0F, 0.02F));
    goalSelector.a(9, new PathfinderGoalRandomStroll(this, 0.6D));
    goalSelector.a(10, new PathfinderGoalLookAtPlayer(this,
        EntityInsentient.class, 8.0F));
  }

}
