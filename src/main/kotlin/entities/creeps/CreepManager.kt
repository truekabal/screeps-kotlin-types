package entities.creeps

import entities.structures.*
import main.kotlin.CREEP_STATE
import main.kotlin.entities.creeps.CreepBase
import main.kotlin.memory.*
import screeps.api.*
import screeps.api.structures.*
import screeps.utils.getOrDefault
import kotlin.math.min

val SPAWNS_AND_EXTENSIONS = arrayOf(STRUCTURE_SPAWN, STRUCTURE_EXTENSION, STRUCTURE_POWER_SPAWN)

class CreepManager(creep: Creep) : CreepBase(creep) {

    companion object {
        fun fillMemory(room: Room, memory: CreepMemory) {
            memory.state = CREEP_STATE.IDLE.ordinal
            memory.room = room.name
        }
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
        if (link != null && link.moreThanHalfCapacity()) {
            target = getTargetToTransferEnergy(link)
            if (target != null) {
                creep.memory.targetID = link.id
                creep.memory.state = CREEP_STATE.WITHDRAW.ordinal
                creep.memory.resource = RESOURCE_ENERGY
                creep.memory.resourceAmount = min(link.energy - link.energyCapacity / 2, creep.emptySpace)
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
            creep.memory.resourceAmount = min(creep.carryCapacity, target.unsafeCast<EnergyContainer>().energyCapacity / 2 - target.unsafeCast<EnergyContainer>().energy)
        } else {
            creep.memory.resourceAmount = 0
        }
        creep.memory.nextTargetID = target.id
        return true
    }

    //----------------------------------------------------------------------------------------------------
    private fun transferPower():Boolean {
        if (creep.isFull) {
            return false
        }

        val powerSpawn = creep.room.find(FIND_MY_STRUCTURES, opts = options { filter = {
                    it.structureType == STRUCTURE_POWER_SPAWN &&
                    it.unsafeCast<StructurePowerSpawn>().power < it.unsafeCast<StructurePowerSpawn>().powerCapacity / 4
        }}).firstOrNull().unsafeCast<StructurePowerSpawn?>() ?: return false

        val powerContainers:Array<Store?> = arrayOf(
            creep.room.storage,
            creep.room.terminal
        )
        for (container:Store? in powerContainers) {
            if (container != null && container.store.getOrDefault(RESOURCE_POWER, 0) > 0) {
                val powerAmount = arrayOf(powerSpawn.requiredPower, container.store[RESOURCE_POWER]!!.toInt(), creep.emptySpace).min()!!
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
        if (link != null && link.lessThanHalfCapacity()) {
            return link
        }

        // ------------  spawns and friends  ------------
        val spawnStuff = creep.room.find(FIND_MY_STRUCTURES, opts = options { filter = {
                        Memory.orders[it.id] == null &&
                        it.structureType in SPAWNS_AND_EXTENSIONS &&
                        !it.unsafeCast<EnergyContainer>().isFull()
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
            it.unsafeCast<StructureTower>().capacityCoef() < 0.75
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
            it.unsafeCast<StructureNuker>().energy < it.unsafeCast<StructureNuker>().energyCapacity
        }}).unsafeCast<StructureNuker>()

        if (nuke != null) {
            return nuke
        }

        if (creep.carry.energy > 0) {
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