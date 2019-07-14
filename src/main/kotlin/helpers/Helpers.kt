package main.kotlin.helpers

import main.kotlin.CREEP_ROLE
import main.kotlin.memory.role
import main.kotlin.memory.sourceID
import screeps.api.*

fun getHarvestersOnSource(sourceID:String):Int {
    return Game.creeps.values.count { it.memory.role == CREEP_ROLE.HARVESTER.ordinal && it.memory.sourceID == sourceID }
}
