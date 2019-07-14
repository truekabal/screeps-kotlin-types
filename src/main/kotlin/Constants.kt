package main.kotlin

import screeps.api.*

val IMPORTANT_BUILD_TYPES: Array<BuildableStructureConstant> = arrayOf(STRUCTURE_WALL, STRUCTURE_RAMPART, STRUCTURE_EXTENSION, STRUCTURE_TOWER)
val STRUCTURES_WITH_STORE: Array<BuildableStructureConstant> = arrayOf(STRUCTURE_STORAGE, STRUCTURE_CONTAINER, STRUCTURE_TERMINAL)


enum class CREEP_ROLE {
    UNDEFINED, //TODO: remove this
    HARVESTER,
    ENERGY_CARRIER,
    UPGRADER,
    BUILDER,
    FIXER,
    CLAIM,
    MANAGER,
}

enum class CREEP_STATE {
    UNDEFINED,  //TODO: change this to IDLE
    HARVEST,
    TRANSFER,
    WITHDRAW,
    IDLE,
    UPGRADE_CONTROLLER,
}
