package main.kotlin.entities.creeps

import main.kotlin.memory.targetID
import main.kotlin.memory.upgrading
import screeps.api.*
import screeps.api.structures.StructureController

class CreepUpgrader(creep: Creep) : CreepBase(creep) {
    companion object {
        fun fillMemory(room:Room, memory: CreepMemory) {
            memory.targetID = room.controller!!.id
        }
    }

    override fun tick() {

        if(creep.memory.upgrading && creep.carry.energy == 0) {
            creep.memory.upgrading = false
            creep.say("🔄 harvest")
        }
        if(!creep.memory.upgrading && creep.carry.energy == creep.carryCapacity) {
            creep.memory.upgrading = true
            creep.say("upgrade")
        }

        if(creep.memory.upgrading) {
            val controller: StructureController? = Game.getObjectById(creep.memory.targetID)
            if(controller != null && creep.upgradeController(controller) == ERR_NOT_IN_RANGE) {
                creep.moveTo(controller.pos)
            }
        } else {
            val tombstones = creep.room.find(FIND_TOMBSTONES, options { filter = { it.store[screeps.api.RESOURCE_ENERGY]!! > 0 }})
            if (tombstones.isNotEmpty()) {
                if (creep.withdraw(tombstones[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(tombstones[0])
                }
                return
            }

            val energy = creep.room.find(FIND_DROPPED_RESOURCES, options {
                filter = {
                    it.resourceType == screeps.api.RESOURCE_ENERGY && it.amount > creep.carryCapacity / 4
                }
            })

            if (energy.isNotEmpty()) {
                if (creep.pickup(energy[0]) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(energy[0])
                }
                return
            }

            val storage = creep.room.storage
            if (storage != null && storage.store[RESOURCE_ENERGY]!! >= creep.carryCapacity) {
                if (creep.withdraw(storage, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(storage)
                }
                return
            }

            val sources = creep.room.find(FIND_SOURCES_ACTIVE)
            sources.sort { a, b -> creep.pos.getRangeTo(a.pos) - creep.pos.getRangeTo(b.pos) }
            if(creep.harvest(sources[0]) == ERR_NOT_IN_RANGE) {
                creep.moveTo(sources[0].pos)
            }
        }
    }


}