package main.kotlin.entities.creeps

import entities.creeps.CreepManager
import entities.isFull
import entities.isNotEmpty
import entities.isNotFull
import main.kotlin.CREEP_ROLE
import main.kotlin.CREEP_STATE
import main.kotlin.memory.*
import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureSpawn
import screeps.api.structures.StructureTower
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import kotlin.math.min

abstract class CreepBase(val creep: Creep) {

    open fun tick() {
        when(creep.memory.state) {
            CREEP_STATE.IDLE.ordinal -> tickIdle()
            CREEP_STATE.HARVEST.ordinal -> harvest()
            CREEP_STATE.TRANSFER.ordinal -> transferResource()
            CREEP_STATE.WITHDRAW.ordinal -> withdrawEnergy()
        }
    }

    fun moveTo(target: NavigationTarget): ScreepsReturnCode {
        return creep.moveTo(target)
    }

    protected fun cleanupMemory() {
        creep.memory.targetID = null
        creep.memory.nextTargetID = null
        creep.memory.resourceAmount = 0
        creep.memory.resource = null
    }

    open fun tickIdle() {

    }

    //----------------------------------------------------------------------------------------------------
    open fun getTargetToWithdrawEnergy():Structure? {
        val obj = creep.pos.findClosestByRange(FIND_MY_STRUCTURES, opts = options {filter = {
            it.structureType == STRUCTURE_LINK || it.structureType == STRUCTURE_STORAGE
        }})

        if (obj != null) {
            if (obj.structureType == STRUCTURE_LINK && obj.unsafeCast<IStore>().store.getUsedCapacity(RESOURCE_ENERGY)!! > 0) {
                return creep.room.storage
            }
        }

        return obj
    }

    open fun getTargetToTransferEnergy(link:StructureLink):Structure? {
        return getTargetToTransferEnergy()
    }

    open fun getTargetToTransferEnergy(source:Source):Structure? {
        // tower
        val tower = creep.pos.findClosestByRange(
            type = FIND_MY_STRUCTURES,
            opts = options { filter = {it.structureType == STRUCTURE_TOWER }}
        ) as StructureTower?
        if (tower != null && Memory.orders[tower.id] == null && tower.store.getUsedCapacity(RESOURCE_ENERGY)!! < tower.store.getCapacity(RESOURCE_ENERGY)!! * 0.75 ) {
            Memory.orders[tower.id] = creep.id
            return tower
        }

        val linkID = creep.room.memory.links.sources[source.id]
        if (linkID != null) {
            val link = Game.getObjectById<StructureLink>(linkID)
            if (link != null && link.store.getFreeCapacity(RESOURCE_ENERGY)!! > 0) {
                return link
            }
        }

        return null
    }

    //------------------------------------------------------------------------------------------------------
    open fun getTargetToTransferEnergy(pos:RoomPosition = creep.pos):Structure? {
        // spawn
        val types = arrayOf(STRUCTURE_SPAWN, STRUCTURE_EXTENSION)
        var target = pos.findClosestByRange(
                type = FIND_MY_STRUCTURES,
                opts = options { filter = {
                    Memory.orders[it.id] == null &&
                    it.structureType in types &&
                    it.unsafeCast<IStore>().isNotFull()
                } }
        )
        if (target != null) {
            Memory.orders[target.id] = creep.id
            return target
        }

        return creep.room.storage
    }

    //------------------------------------------------------------------------------------------------------
    open fun getTargetToTransferResource():Structure? {
        return null
    }

    //------------------------------------------------------------------------------------------------------
    protected fun withdrawEnergy() {
        val target:Structure? = Game.getObjectById(creep.memory.targetID)
        if (target == null) {
            cleanupMemory()
            creep.memory.state = CREEP_STATE.IDLE.ordinal
            tickIdle()
            return
        }

        val result = creep.withdraw(target, creep.memory.resource!!, min(creep.memory.resourceAmount, creep.store.getFreeCapacity()))
        when(result) {
            ERR_NOT_IN_RANGE -> moveTo(target)
            ERR_NOT_ENOUGH_RESOURCES -> {
                cleanupMemory()
                creep.memory.state = CREEP_STATE.IDLE.ordinal
            }
            ERR_FULL, OK -> onWithdraw()
        }
    }

    //------------------------------------------------------------------------------------------------------
    protected fun transferResource() {
        if (creep.store.isEmpty()) {
            onTransferComplete()
            return
        }

        val resource:ResourceConstant
        if (creep.memory.resource != null) {
            resource = creep.memory.resource!!
            if (creep.store.getUsedCapacity(resource) == 0) {
                onTransferComplete()
                return
            }
        } else {
            resource = RESOURCE_ENERGY
        }


        var target: Structure?
        if (creep.memory.targetID == null) {
            target = getTargetToTransferEnergy(creep.pos)
            if (target == null) {
                return
            }
            creep.memory.targetID = target.id
        } else {
            target = Game.getObjectById(creep.memory.targetID)
            if (target == null) {
                creep.memory.targetID = null
                target = getTargetToTransferEnergy(creep.pos)
                if (target == null) {
                    return
                }

                creep.memory.targetID = target.id
            }

        }

        val result = creep.transfer(target, resource) //TODO: resourceAmount
        when(result) {
            ERR_NOT_IN_RANGE -> creep.moveTo(target)
            ERR_FULL -> {
                delete(Memory.orders[creep.memory.targetID!!])
                creep.memory.targetID = null
                return
            }
            ERR_NOT_ENOUGH_RESOURCES, OK -> {
                delete(Memory.orders[creep.memory.targetID!!])
                creep.memory.targetID = null
                onTransferComplete()
            }
        }
    }

    //------------------------------------------------------------------------------------------------------
    protected fun harvest() {
        if (creep.isFull()) {
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

        val result = creep.harvest(target)
        when (result) {
            ERR_NOT_ENOUGH_RESOURCES, ERR_NOT_IN_RANGE -> creep.moveTo(target)
        }
    }

    protected open fun onTransferComplete() {}
    protected open fun onHarvestFinished() {}
    protected open fun onWithdraw() {}

}

fun getInitialCreepMemory(spawn:StructureSpawn, role:Int, room:Room = spawn.room):CreepMemory {
    val memory = screeps.utils.unsafe.jsObject<CreepMemory> { this.role = role }
    when(role) {
        CREEP_ROLE.CLAIM.ordinal -> CreepClaim.fillMemory(memory)
        CREEP_ROLE.HARVESTER.ordinal -> CreepHarvester.fillMemory(room, memory)
        CREEP_ROLE.UPGRADER.ordinal -> CreepUpgrader.fillMemory(room, memory)
        CREEP_ROLE.BUILDER.ordinal -> CreepBuilder.fillMemory(room, memory)
        CREEP_ROLE.MANAGER.ordinal -> CreepManager.fillMemory(room, memory)
        CREEP_ROLE.FIXER.ordinal -> CreepFixer.fillMemory(room, memory)
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
        CREEP_ROLE.MANAGER.ordinal      -> CreepManager(this).tick()
    }
}
