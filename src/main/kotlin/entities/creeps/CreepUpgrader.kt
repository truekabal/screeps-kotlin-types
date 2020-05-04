package main.kotlin.entities.creeps

import entities.isEmpty
import entities.isFull
import entities.isNotEmpty
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
        if(creep.memory.upgrading && creep.isEmpty()) {
            creep.memory.upgrading = false
            creep.say("🔄 harvest")
        }
        if(!creep.memory.upgrading && creep.isFull()) {
            creep.memory.upgrading = true
            creep.say("upgrade")
        }

        if(creep.memory.upgrading) {
            if(creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                creep.moveTo(controller.pos)
            }
        } else {
            val link: StructureLink? = Game.getObjectById(creep.room.memory.links.controller)
            if (link != null && link.store.getUsedCapacity(RESOURCE_ENERGY)!! > 0) {
                if (creep.withdraw(link, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(link)
                }
                return
            }

            val storage = controller.room.storage
            if (storage != null && storage.store.getUsedCapacity(RESOURCE_ENERGY)!! >= creep.store.getCapacity()) {
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