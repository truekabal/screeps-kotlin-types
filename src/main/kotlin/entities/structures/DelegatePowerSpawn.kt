package main.kotlin.entities.structures

import screeps.api.structures.StructurePowerSpawn

class DelegatePowerSpawn(private val spawn: StructurePowerSpawn) {

    fun tick() {
        spawn.processPower()
    }

}