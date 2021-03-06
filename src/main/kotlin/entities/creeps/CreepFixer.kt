package main.kotlin.entities.creeps

import entities.isEmpty
import entities.isFull
import main.kotlin.memory.room
import main.kotlin.memory.targetID
import main.kotlin.memory.upgrading
import screeps.api.*
import screeps.api.structures.Structure

class CreepFixer(creep: Creep) : CreepBase(creep) {
    companion object {
        fun fillMemory(room:Room, memory: CreepMemory) {
            memory.room = room.name
        }
    }

    override fun tick() {
        var room = Game.rooms[creep.memory.room]
        if (room == null) {
            creep.say("Room ${creep.memory.room} unavailable")
            room = creep.room
        }

        if(creep.memory.upgrading && creep.isEmpty()) {
            creep.memory.upgrading = false;
            creep.memory.targetID = null
            creep.say("🔄 harvest")
        }
        if(!creep.memory.upgrading && creep.isFull()) {
            creep.memory.upgrading = true;
            creep.say("⚡ repair");
        }

        if(creep.memory.upgrading) {
            var target:Structure? = null
            if (creep.memory.targetID != null) {
                target = Game.getObjectById(creep.memory.targetID)
            }

            if (target == null) {
                val targets = room.find(FIND_STRUCTURES, options { filter = { it.hitsMax - it.hits > 200 } })
                targets.sort {a, b -> (b.hitsMax - b.hits) - (a.hitsMax - a.hits) }
                if (targets.isNotEmpty()) {
                    target = targets[0]
                    creep.memory.targetID = target.id
                }
            }

            if (target != null) {
                if (creep.repair(target) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(target)
                } else if (target.hits == target.hitsMax) {
                    creep.memory.targetID = null
                }
            }

        } else {
            val tombstones = room.find(FIND_TOMBSTONES, options { filter = { it.store[RESOURCE_ENERGY]!! > 0 }})
            if (tombstones.isNotEmpty()) {
                if (creep.withdraw(tombstones[0], RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(tombstones[0])
                }
                return
            }

            val energy = room.find(FIND_DROPPED_RESOURCES, options {
                filter = {
                    it.resourceType == RESOURCE_ENERGY && it.amount > creep.store.getCapacity()!! / 4
                }
            })

            if (energy.isNotEmpty()) {
                if (creep.pickup(energy[0]) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(energy[0])
                }
                return
            }

            val storage = room.storage
            if (storage != null && storage.store.getUsedCapacity(RESOURCE_ENERGY)!! >= creep.store.getCapacity()) {
                if (creep.withdraw(storage, RESOURCE_ENERGY) == ERR_NOT_IN_RANGE) {
                    creep.moveTo(storage)
                }
                return
            }

            val sources = room.find(FIND_SOURCES_ACTIVE)
            sources.sort { a, b -> creep.pos.getRangeTo(a.pos) - creep.pos.getRangeTo(b.pos) }
            if(creep.harvest(sources[0]) == ERR_NOT_IN_RANGE) {
                creep.moveTo(sources[0].pos)
            }
        }
    }
}