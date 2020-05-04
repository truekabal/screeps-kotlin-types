package entities.creeps

import entities.*
import main.kotlin.CREEP_STATE
import main.kotlin.entities.creeps.CreepBase
import main.kotlin.memory.*
import screeps.api.*
import screeps.api.structures.*
import screeps.utils.getOrDefault
import screeps.utils.isNotEmpty
import kotlin.math.min

val SPAWNS_AND_EXTENSIONS = arrayOf(STRUCTURE_SPAWN, STRUCTURE_EXTENSION, STRUCTURE_POWER_SPAWN)

class CreepManager(creep: Creep) : CreepBase(creep) {

    companion object {
        fun fillMemory(room: Room, memory: CreepMemory) {
            memory.state = CREEP_STATE.IDLE.ordinal
            memory.room = room.name
        }
    }

    private fun hasSourceLinks():Boolean {
        return creep.room.memory.links.sources.isNotEmpty() && creep.room.memory.links.sources.values.any { Game.getObjectById<StructureLink>(it) != null }
    }

    private fun getStorageLink():StructureLink? {
        return Game.getObjectById(creep.room.memory.links.storage)
    }

    //----------------------------------------------------------------------------------------------------
    override fun tickIdle() {
        if (transferPower()) {
            tick()
            return
        }

        if (transferEnergy())  {
            tick()
            return
        }

        if (transferResources()) {
            tick()
            return
        }
    }

    //----------------------------------------------------------------------------------------------------
    private fun transferEnergy():Boolean {
        // check withdraw from link
        val link:StructureLink? = getStorageLink()
        var target:Structure?
        if (link != null && link.store.getUsedCapacity(RESOURCE_ENERGY)!! > link.store.getCapacity(RESOURCE_ENERGY)!! / 2) {
            target = getTargetToTransferEnergy(link)
            if (target != null) {
                creep.memory.targetID = link.id
                creep.memory.state = CREEP_STATE.WITHDRAW.ordinal
                creep.memory.resource = RESOURCE_ENERGY

                creep.memory.resourceAmount = min(
                    link.store.getUsedCapacity(RESOURCE_ENERGY)!! - link.store.getCapacity(RESOURCE_ENERGY)!! / 2,
                    creep.store.getFreeCapacity()
                )

                creep.memory.nextTargetID = target.id
                return true
            }
        }

        val source: StructureStorage = creep.room.storage ?: return false

        // нашли куда нести
        target = getTargetToTransferEnergy()
        if (target == null) {
            return false
        }

        creep.memory.targetID = source.id
        creep.memory.state = CREEP_STATE.WITHDRAW.ordinal
        creep.memory.resource = RESOURCE_ENERGY
        if (target.structureType == STRUCTURE_LINK) {
            creep.memory.resourceAmount = min(
                creep.store.getCapacity()!!,
                target.unsafeCast<StoreOwner>().store.getCapacity(RESOURCE_ENERGY)!! / 2 - target.unsafeCast<StoreOwner>().store.getUsedCapacity(RESOURCE_ENERGY)!!
            )
        } else {
            creep.memory.resourceAmount = 0
        }
        creep.memory.nextTargetID = target.id
        return true
    }

    //----------------------------------------------------------------------------------------------------
    private fun transferPower():Boolean {
        if (creep.isFull()) {
            return false
        }

        val powerSpawn = creep.room.find(FIND_MY_STRUCTURES, opts = options { filter = {
                    it.structureType == STRUCTURE_POWER_SPAWN &&
                    it.unsafeCast<StructurePowerSpawn>().store.getUsedCapacity(RESOURCE_POWER)!! < it.unsafeCast<StructurePowerSpawn>().store.getCapacity(RESOURCE_POWER)!! / 4
        }}).firstOrNull().unsafeCast<StructurePowerSpawn?>() ?: return false

        val powerContainers:Array<StoreOwner?> = arrayOf(
            creep.room.storage,
            creep.room.terminal
        )
        for (container:StoreOwner? in powerContainers) {
            if (container != null && container.store.getUsedCapacity(RESOURCE_POWER)!! > 0) {
                val powerAmount = arrayOf(
                    powerSpawn.store.getFreeCapacity(RESOURCE_POWER)!!,
                    container.store.getUsedCapacity(RESOURCE_POWER)!!.toInt(),
                    creep.store.getFreeCapacity()
                ).min()!!
                creep.memory.state = CREEP_STATE.WITHDRAW.ordinal
                creep.memory.targetID = container.unsafeCast<Structure>().id
                creep.memory.resourceAmount = powerAmount
                creep.memory.resource = RESOURCE_POWER
                creep.memory.nextTargetID = powerSpawn.id
                return true
            }
        }

        return false
    }

    //----------------------------------------------------------------------------------------------------
    private fun transferResources():Boolean {
        return false
    }

    //----------------------------------------------------------------------------------------------------
    override fun getTargetToTransferEnergy(link: StructureLink):Structure? {
        val storage = creep.room.storage
        if (storage != null && !storage.isFull()) {
            return storage
        }

        return getTargetToTransferEnergy()
    }

    //----------------------------------------------------------------------------------------------------
    override fun getTargetToTransferEnergy(pos: RoomPosition): Structure? {
        val link:StructureLink? = getStorageLink()
        if (link != null && link.store.getUsedCapacity(RESOURCE_ENERGY)!! < link.store.getCapacity(RESOURCE_ENERGY)!! / 2) {
            return link
        }

        // ------------  spawns and friends  ------------
        val spawnStuff = creep.room.find(FIND_MY_STRUCTURES, opts = options { filter = {
                        Memory.orders[it.id] == null &&
                        it.structureType in SPAWNS_AND_EXTENSIONS &&
                        it.unsafeCast<StoreOwner>().store.getFreeCapacity(RESOURCE_ENERGY)!! > 0
                }
            }
        )
        if (spawnStuff.isNotEmpty()) {
            spawnStuff.sortBy {
                if (it.structureType == STRUCTURE_POWER_SPAWN)
                        Int.MAX_VALUE
                else {
                    it.pos.getRangeTo(creep)
                }
            }
            return spawnStuff[0]
        }

        // ------------  labs  ------------
        val labs = creep.room.find(FIND_MY_STRUCTURES, opts = options {filter = {it.structureType == STRUCTURE_LAB}}).unsafeCast<Array<StructureLab>>()
        if (labs.isNotEmpty()) {
            for (lab in labs) {
                if (!lab.isFull()) {
                    return lab
                }
            }
        }

        // ------------  towers  ------------
        val tower = creep.pos.findClosestByRange(type = FIND_MY_STRUCTURES, opts = options { filter = {
            it.structureType == STRUCTURE_TOWER &&
            Memory.orders[it.id] == null &&
            it.unsafeCast<StructureTower>().capacityCoef(RESOURCE_ENERGY) < 0.75
        }})
        if (tower != null) {
            return tower
        }

        val terminal = creep.room.terminal
        if (terminal != null && !terminal.isFull() && terminal.store.getOrDefault(RESOURCE_ENERGY, 0) < 100000) {
            return terminal
        }

        val nuke:StructureNuker? = creep.pos.findClosestByRange(type = FIND_MY_STRUCTURES, opts = options {filter = {
            it.structureType == STRUCTURE_NUKER &&
            it.unsafeCast<StructureNuker>().store.getFreeCapacity(RESOURCE_ENERGY)!! > 0
        }}).unsafeCast<StructureNuker>()

        if (nuke != null) {
            return nuke
        }

        if (creep.isNotEmpty()) {
            return creep.room.storage
        }

        return null
    }

    override fun onWithdraw() {
        creep.memory.state = CREEP_STATE.TRANSFER.ordinal
        creep.memory.targetID = creep.memory.nextTargetID
        creep.memory.nextTargetID = null
    }

    override fun onTransferComplete() {
//        if (creep.carry.energy > 0) {
//            val target = getTargetToTransferEnergy()
//            if (target != null) {
//                creep.memory.resourceAmount = 0
//                creep.memory.targetID = target.id
//                creep.moveTo(target)
//                return
//            }
//        }
        cleanupMemory()
        creep.memory.state = CREEP_STATE.IDLE.ordinal
//        tickIdle()
    }
}