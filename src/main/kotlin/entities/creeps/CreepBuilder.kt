package main.kotlin.entities.creeps

import main.kotlin.IMPORTANT_BUILD_TYPES
import main.kotlin.memory.building
import main.kotlin.memory.targetID
import screeps.api.*
import screeps.api.structures.StructureController

class CreepBuilder(creep: Creep) : CreepBase(creep) {
    companion object {
        fun fillMemory(room:Room, memory: CreepMemory) {
            memory.building = false
        }
    }

    override fun tick() {
        if(creep.memory.building && creep.carry.energy == 0) {
            creep.memory.building = false
            creep.say("🔄 harvest")
        }
        if(!creep.memory.building && creep.carry.energy == creep.carryCapacity) {
            creep.memory.building = true
            creep.say("\uD83D\uDEA7 build")
        }

        if(creep.memory.building) {
            var building = Game.getObjectById<ConstructionSite>(creep.memory.targetID)
            if (building == null) {
                building = getNearestToBuild()
                if (building != null) {
                    creep.memory.targetID = building.id
                } else {
                    creep.suicide()
                    return
                }
            }

            if (creep.build(building) == ERR_NOT_IN_RANGE) {
                creep.moveTo(building)
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

    private fun getNearestToBuild():ConstructionSite? {
        val importantBuildings = creep.room.find(FIND_CONSTRUCTION_SITES,
                options {filter = { IMPORTANT_BUILD_TYPES.contains(it.structureType) } }
        )
        if (importantBuildings.isNotEmpty()) {
            importantBuildings.sort { a, b -> creep.pos.getRangeTo(a.pos) - creep.pos.getRangeTo(b.pos) }
            return importantBuildings[0]
        }

        val buildings = creep.room.find(FIND_CONSTRUCTION_SITES)
        if (buildings.isNotEmpty()) {
            buildings.sort {a,b ->
                when {
                    a.progressTotal < b.progressTotal -> -1
                    b.progressTotal < a.progressTotal -> 1
                    else -> b.progress / b.progressTotal - a.progress / a.progressTotal
                }
            }

            return buildings[0]
        }
        return null
    }

}