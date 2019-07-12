package entities.creeps

import entities.structures.capacityCoef
import entities.structures.isFull
import entities.structures.lessThanHalfCapacity
import entities.structures.moreThanHalfCapacity
import main.kotlin.CREEP_STATE
import main.kotlin.entities.creeps.CreepBase
import main.kotlin.memory.*
import screeps.api.*
import screeps.api.structures.Structure
import screeps.api.structures.StructureLab
import screeps.api.structures.StructureLink
import screeps.api.structures.StructureTower
import screeps.utils.getOrDefault

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

    override fun tickIdle() {
        // нашли источник ресов
        val link:StructureLink? = getStorageLink()
        var target:Structure?
        // check withdraw from link
        if (link != null && link.moreThanHalfCapacity()) {
            target = getTargetToTransferEnergy(link)
            if (target != null) {
                creep.memory.targetID = link.id
                creep.memory.state = CREEP_STATE.WITHDRAW_ENERGY.ordinal
                creep.memory.resource = RESOURCE_ENERGY
                creep.memory.resourceAmount = link.energy - link.energyCapacity / 2
                creep.memory.nextTargetID = target.id
                tick()
                return
            }
        }

        val source:Structure? = creep.room.storage
        if (source == null) {
            return
        }

        // нашли куда нести
        target = getTargetToTransferEnergy()
        if (target == null) {
            return
        }

        // записали двойную задачу
        // выполнили
        // повторили
        creep.memory.targetID = source.id
        creep.memory.state = CREEP_STATE.WITHDRAW_ENERGY.ordinal
        creep.memory.resource = RESOURCE_ENERGY
        if (target.structureType == STRUCTURE_LINK) {
            creep.memory.resourceAmount = target.unsafeCast<EnergyContainer>().energyCapacity / 2 - target.unsafeCast<EnergyContainer>().energy
        } else {
            creep.memory.resourceAmount = 0
        }
        creep.memory.nextTargetID = target.id
        tick()
    }

    private fun checkLinkToStorage():Boolean {
        val link = (if (creep.room.memory.links.storage == null) null else Game.getObjectById(creep.room.memory.links.storage)) as StructureLink?
            ?: return false
        val storage = creep.room.storage
            ?: return false

        if (link.energy > link.energyCapacity / 2) {
            return true
        }

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

    override fun getTargetToTransferEnergy(pos: RoomPosition): Structure? {
        val link:StructureLink? = getStorageLink()
        if (link != null && link.lessThanHalfCapacity()) {
            return link
        }

        // ------------  spawns and friends  ------------
        val spawnStuff = pos.findClosestByRange(
            type = FIND_MY_STRUCTURES,
            opts = options { filter = {
                        Memory.orders[it.id] == null &&
                        it.structureType in SPAWNS_AND_EXTENSIONS &&
                        !it.unsafeCast<EnergyContainer>().isFull()
                }
            }
        )
        if (spawnStuff != null) {
            return spawnStuff
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
        val towers = creep.room.find(FIND_MY_STRUCTURES, opts = options { filter = {it.structureType == STRUCTURE_TOWER }})
        if (towers.isNotEmpty()) {
            for (tower in towers) {
                if (Memory.orders[tower.id] == null && tower.unsafeCast<StructureTower>().capacityCoef() < 0.75) {
                    return tower
                }
            }
        }

        val terminal = creep.room.terminal
        if (terminal != null && !terminal.isFull() && terminal.store.getOrDefault(RESOURCE_ENERGY, 0) < 100000) {
            return terminal
        }

        if (creep.carry.energy > 0) {
            return creep.room.storage
        }

        return null
    }

    override fun onEnergyWithdraw() {
        creep.memory.state = CREEP_STATE.TRANSFER_ENERGY.ordinal
        creep.memory.targetID = creep.memory.nextTargetID
        creep.memory.nextTargetID = null
    }

    override fun onResourceTransferComplete() {
        if (creep.carry.energy > 0) {
            val target = getTargetToTransferEnergy()
            if (target != null) {
                creep.memory.targetID = target.id
                return
            }
        }
        cleanupMemory()
        creep.memory.state = CREEP_STATE.IDLE.ordinal
        tickIdle()
    }
}