package main.kotlin.entities.creeps

import main.kotlin.CREEP_ROLE
import main.kotlin.CREEP_STATE
import main.kotlin.memory.orders
import main.kotlin.memory.role
import main.kotlin.memory.state
import main.kotlin.memory.targetID
import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureSpawn
import screeps.api.structures.StructureTower
import screeps.utils.unsafe.delete

abstract class CreepBase(val creep: Creep) {
    open fun tick() {
        when(creep.memory.state) {
            CREEP_STATE.HARVEST.ordinal -> harvest()
            CREEP_STATE.RETURN_ENERGY.ordinal -> returnEnergy()
        }
    }

    //----------------------------------------------------------------------------------------------------
    // used by harvesters, builders, repairers
    fun getTargetToReturnEnergy(pos:RoomPosition = creep.pos):Structure? {
        // spawn
        var types = hashSetOf(STRUCTURE_SPAWN, STRUCTURE_EXTENSION)
        var target = pos.findClosestByRange(
                type = FIND_MY_STRUCTURES,
                opts = options { filter = {
                    types.contains(it.structureType) &&
                            (it.unsafeCast<EnergyContainer>()).energy < (it.unsafeCast<EnergyContainer>()).energyCapacity
                } }
        )
        if (target != null) return target

        // tower
        val tower = pos.findClosestByRange(
                type = FIND_STRUCTURES,
                opts = options { filter = {it.structureType == STRUCTURE_TOWER }}
        ) as StructureTower?
        if (tower != null && Memory.orders[tower.id] == null && tower.energy + creep.carryCapacity <= tower.energyCapacity ) {
            Memory.orders[tower.id] = creep.id
            return tower
        }

        // storage
        types = hashSetOf(STRUCTURE_STORAGE, STRUCTURE_CONTAINER)
        target = pos.findClosestByRange(
                type = FIND_MY_STRUCTURES,
                opts = options { filter = { types.contains(it.structureType) } }
        )
        return target
    }

    //------------------------------------------------------------------------------------------------------
    protected fun returnEnergy() {
        if (creep.carry.energy == 0) {
            onEnergyReturn()
            return
        }

        var target: Structure?
        if (creep.memory.targetID == null) {
            target = getTargetToReturnEnergy(creep.pos)
            if (target == null) {
                console.log("nowhere to return energy. creep " + creep.name + " room " + creep.room.name)
                return
            }
            creep.memory.targetID = target.id
        } else {
            target = Game.getObjectById(creep.memory.targetID)
            if (target == null) {
                creep.memory.targetID = null
                target = getTargetToReturnEnergy(creep.pos)
                if (target == null) {
                    console.log("nowhere to return energy. creep " + creep.name + " room " + creep.room.name)
                    return
                }

                creep.memory.targetID = target.id
            }

        }

        val result = creep.transfer(target, RESOURCE_ENERGY)
        when(result) {
            ERR_NOT_IN_RANGE -> creep.moveTo(target)
            ERR_FULL -> {
                delete(Memory.orders[creep.memory.targetID!!])
                creep.memory.targetID = null
//                returnEnergy()
                return
            }
            OK -> {
                delete(Memory.orders[creep.memory.targetID!!])
                creep.memory.targetID = null
            }
        }
    }

    //------------------------------------------------------------------------------------------------------
    protected fun harvest() {
        if (creep.carry.energy == creep.carryCapacity) {
            onHarvestFinished()
            return
        }

        var target:Source? = Game.getObjectById(creep.memory.targetID)
        if (target == null) {
            target = creep.pos.findClosestByRange(FIND_SOURCES)
            if (target == null) {
                console.log("no sources for " + creep.name + " in " + creep.room.name)
                return
            }
            creep.memory.targetID = target.id
        }

        if (creep.harvest(target) == ERR_NOT_IN_RANGE) {
            creep.moveTo(target)
        }
    }

    protected open fun onEnergyReturn() {}
    protected open fun onHarvestFinished() {}

}

fun getInitialCreepMemory(spawn:StructureSpawn, role:Int, room:Room = spawn.room):CreepMemory {
    val memory = screeps.utils.unsafe.jsObject<CreepMemory> { this.role = role }
    when(role) {
        CREEP_ROLE.CLAIM.ordinal -> CreepClaim.fillMemory(memory)
        CREEP_ROLE.HARVESTER.ordinal -> CreepHarvester.fillMemory(room, memory)
        CREEP_ROLE.UPGRADER.ordinal -> CreepUpgrader.fillMemory(room, memory)
        CREEP_ROLE.BUILDER.ordinal -> CreepBuilder.fillMemory(room, memory)
    }
    return memory
}

fun Creep.tick() {
    when (memory.role) {
        CREEP_ROLE.HARVESTER.ordinal    -> CreepHarvester(this).tick()
        CREEP_ROLE.FIXER.ordinal        -> CreepFixer(this).tick()
        CREEP_ROLE.UPGRADER.ordinal     -> CreepUpgrader(this).tick()
        CREEP_ROLE.CLAIM.ordinal        -> CreepClaim(this).tick()
        CREEP_ROLE.BUILDER.ordinal      -> CreepBuilder(this).tick()
    }
}
