/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2017 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.compat.v1_8_R3.entity.ai.target;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.ai.AIGoal;
import de.Keyle.MyPet.api.entity.ai.target.TargetPriority;
import de.Keyle.MyPet.api.skill.skills.BehaviorInfo.BehaviorState;
import de.Keyle.MyPet.api.util.Compat;
import de.Keyle.MyPet.compat.v1_8_R3.entity.EntityMyPet;
import de.Keyle.MyPet.compat.v1_8_R3.entity.ai.movement.Control;
import de.Keyle.MyPet.skill.skills.Behavior;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityTameableAnimal;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Compat("v1_8_R3")
public class ControlTarget implements AIGoal {
    private MyPet myPet;
    private EntityMyPet petEntity;
    private EntityLiving target;
    private float range;
    private Control controlPathfinderGoal;

    public ControlTarget(EntityMyPet petEntity, float range) {
        this.petEntity = petEntity;
        this.myPet = petEntity.getMyPet();
        this.range = range;
    }

    @Override
    public boolean shouldStart() {
        if (controlPathfinderGoal == null) {
            if (petEntity.getPathfinder().hasGoal("Control")) {
                controlPathfinderGoal = (Control) petEntity.getPathfinder().getGoal("Control");
            }
        }
        if (controlPathfinderGoal == null) {
            return false;
        }
        if (myPet.getDamage() <= 0 && myPet.getRangedDamage() <= 0) {
            return false;
        }
        if (controlPathfinderGoal.moveTo != null && petEntity.canMove()) {
            Behavior behaviorSkill = null;
            if (myPet.getSkills().isSkillActive(Behavior.class)) {
                behaviorSkill = myPet.getSkills().getSkill(Behavior.class).get();
                if (behaviorSkill.getBehavior() == Behavior.BehaviorState.Friendly) {
                    return false;
                }
            }
            for (Object entityObj : this.petEntity.world.a(EntityLiving.class, this.petEntity.getBoundingBox().grow((double) this.range, 4.0D, (double) this.range))) {
                EntityLiving entityLiving = (EntityLiving) entityObj;

                if (entityLiving != petEntity && !(entityLiving instanceof EntityArmorStand)) {
                    if (entityLiving instanceof EntityPlayer) {
                        Player targetPlayer = (Player) entityLiving.getBukkitEntity();
                        if (myPet.getOwner().equals(targetPlayer)) {
                            continue;
                        } else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetPlayer, true)) {
                            continue;
                        }
                    } else if (entityLiving instanceof EntityTameableAnimal) {
                        EntityTameableAnimal tameable = (EntityTameableAnimal) entityLiving;
                        if (tameable.isTamed() && tameable.getOwner() != null) {
                            Player tameableOwner = (Player) tameable.getOwner().getBukkitEntity();
                            if (myPet.getOwner().equals(tameableOwner)) {
                                continue;
                            } else if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), tameableOwner, true)) {
                                continue;
                            }
                        }
                    } else if (entityLiving instanceof EntityMyPet) {
                        MyPet targetMyPet = ((EntityMyPet) entityLiving).getMyPet();
                        if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), targetMyPet.getOwner().getPlayer(), true)) {
                            continue;
                        }
                    }
                    if (!MyPetApi.getHookHelper().canHurt(myPet.getOwner().getPlayer(), entityLiving.getBukkitEntity())) {
                        continue;
                    }
                    if (behaviorSkill != null) {
                        if (behaviorSkill.getBehavior() == BehaviorState.Raid) {
                            if (entityLiving instanceof EntityTameableAnimal) {
                                continue;
                            } else if (entityLiving instanceof EntityMyPet) {
                                continue;
                            } else if (entityLiving instanceof EntityPlayer) {
                                continue;
                            }
                        }
                    }
                    controlPathfinderGoal.stopControl();
                    this.target = entityLiving;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shouldFinish() {
        if (!petEntity.canMove()) {
            return true;
        }
        if (!petEntity.hasTarget()) {
            return true;
        }

        EntityLiving target = ((CraftLivingEntity) this.petEntity.getTarget()).getHandle();

        if (target.world != petEntity.world) {
            return true;
        } else if (petEntity.h(target) > 400) {
            return true;
        } else if (petEntity.h(((CraftPlayer) petEntity.getOwner().getPlayer()).getHandle()) > 600) {
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        petEntity.setTarget((LivingEntity) this.target.getBukkitEntity(), TargetPriority.Control);
    }

    @Override
    public void finish() {
        petEntity.forgetTarget();
    }
}