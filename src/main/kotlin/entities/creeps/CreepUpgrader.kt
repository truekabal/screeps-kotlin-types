package main.kotlin.entities.creeps

import main.kotlin.memory.controller
import main.kotlin.memory.links
import main.kotlin.memory.targetID
import main.kotlin.memory.upgrading
import screeps.api.*
import screeps.api.structures.StructureController
import screeps.api.structures.StructureLink

class CreepUpgrader(creep: Creep) : CreepBase(creep) {
    companion object {
        fun fillMemory(room:Room, memory: CreepMemory) {
            memory.targetID = room.controller!!.id
        }
    }

    override fun tick() {
        val controller = Game.getObjectById<StructureController>(creep.memory.targetID)!!
        if(creep.memory.upgrading && creep.carry.energy == 0) {
            creep.memory.upgrading = false
            creep.say("🔄 harvest")
        }
        if(!creep.memory.upgrading && creep.carry.energy == creep.carryCapacity) {
            creep.memory.upgrading = true
            creep.say("upgrade")
        }

        if(creep.memory.upgrading) {
            if(creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                creep.moveTo(controller.pos)
            }
        } else {
            val link: StructureLink? = Game.getObjectById(creep.room.memory.links.controller)
            if (link != null && link.energy > 0) {
                if (creep.withdraw(link, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(link)
                }
                return
            }

            val storage = controller.room.storage
            if (storage != null && storage.store[RESOURCE_ENERGY]!! >= creep.carryCapacity) {
                if (creep.withdraw(storage, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(storage)
                }
                return
            }

            val sources = controller.room.find(FIND_SOURCES_ACTIVE)
            sources.sort { a, b -> creep.pos.getRangeTo(a.pos) - creep.pos.getRangeTo(b.pos) }
            if(creep.harvest(sources[0]) == ERR_NOT_IN_RANGE) {
                creep.moveTo(sources[0].pos)
            }
        }
    }


}