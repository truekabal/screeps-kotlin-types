package main.kotlin.entities.creeps

import main.kotlin.CREEP_STATE
import main.kotlin.helpers.getHarvestersOnSource
import main.kotlin.memory.sourceID
import main.kotlin.memory.state
import main.kotlin.memory.targetID
import screeps.api.*
import screeps.api.structures.Structure

class CreepHarvester(creep: Creep) : CreepBase(creep) {
    companion object {
        fun fillMemory(room:Room, memory: CreepMemory) {
            val sources = room.find(FIND_SOURCES)
            if (sources.isEmpty()) return
            val source = sources.minBy { getHarvestersOnSource(it.id) }!!
            memory.targetID = source.id
            memory.sourceID = source.id
            memory.state = CREEP_STATE.HARVEST.ordinal
        }
    }

    override fun onTransferComplete() {
        creep.memory.targetID = creep.memory.sourceID
        creep.memory.state = CREEP_STATE.HARVEST.ordinal
    }

    override fun getTargetToTransferEnergy(pos: RoomPosition): Structure? {
        return getTargetToTransferEnergy(Game.getObjectById<Source>(creep.memory.sourceID)!!) ?:
               super.getTargetToTransferEnergy(pos)
    }

    override fun onHarvestFinished() {
        val target = getTargetToTransferEnergy()
        creep.memory.state = CREEP_STATE.TRANSFER.ordinal
        if (target != null) {
            creep.memory.targetID = target.id
            transferResource()
        } else {
            creep.memory.targetID = null
        }
    }
}