package main.kotlin.helpers

import main.kotlin.CREEP_ROLE
import main.kotlin.memory.role
import main.kotlin.memory.sourceID
import screeps.api.*

fun getHarvestersOnSource(sourceID:String?):Int {
    if (sourceID == null || sourceID.isEmpty()) return 0
    val src:Source? = Game.getObjectById(sourceID)
    if (src == null) return 0
    return Game.creeps.values.sumBy { creep ->
        if (creep.memory.role == CREEP_ROLE.HARVESTER.ordinal &&
        creep.memory.sourceID == sourceID) {
            1
        } else {
            0
        }
    }
}

fun getEnergyContainers(obj: RoomObject) {
    if (Memory[obj.room!!.name + "lockEnergy"] == true) return

    val energySources = arrayOf(
            STRUCTURE_STORAGE,
            STRUCTURE_CONTAINER,
            STRUCTURE_SPAWN,
            STRUCTURE_EXTENSION
    )

    val sources = obj.room!!.find(FIND_STRUCTURES)
    for (structureType in energySources) {
        for (s in sources) {
            if (s.structureType == structureType && s.unsafeCast<EnergyContainer>().energy > 10) {

            }
        }
    }
}
