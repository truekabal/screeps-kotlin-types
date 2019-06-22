package main.kotlin.entities.creeps

import main.kotlin.entities.creeps.CreepBase
import main.kotlin.memory.room
import screeps.api.*

class CreepClaim(creep:Creep): CreepBase(creep) {
    companion object {
        fun fillMemory(memory: CreepMemory) {
            memory.room = "" //TODO: room
        }
    }

    private fun isInDesiredRoom():Boolean {
        val room = Game.rooms[creep.memory.room]
        return room != null && room.name == creep.memory.room
    }

    override fun tick() {
        if (isInDesiredRoom()) {
            val target = creep.room.controller!!
            val result = creep.claimController(target)
            if (result == ERR_NOT_IN_RANGE) {
                creep.moveTo(target)
            } else if (result == ERR_NOT_OWNER) {
                creep.attackController(target)
            }
            return
        }

        creep.moveTo(RoomPosition(25, 25, creep.memory.room))
    }
}