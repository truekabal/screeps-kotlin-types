package main.kotlin.entities.structures

import screeps.api.STRUCTURE_SPAWN
import screeps.api.STRUCTURE_TOWER
import screeps.api.structures.Structure
import screeps.api.structures.StructureSpawn
import screeps.api.structures.StructureTower

abstract class StructureBase(val structure: Structure) {
    abstract fun tick()
}


fun Structure.tick() {
    when(structureType) {
        STRUCTURE_TOWER -> DelegateTower(this as StructureTower).tick()
        STRUCTURE_SPAWN -> DelegateSpawn(this as StructureSpawn).tick()
    }
}
