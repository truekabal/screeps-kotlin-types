package main.kotlin

import main.kotlin.memory.orders
import main.kotlin.memory.timer
import screeps.api.*
import screeps.api.Game.creeps
import screeps.utils.unsafe.delete

fun GC() {
    try {
        if (Memory.timer % GC_TICK_TIMEOUT == 0) {

            // clean creeps
            for ((creepName, _) in Memory.creeps) {
                if (creeps[creepName] == null) {
                    delete(Memory.creeps[creepName])
                }
            }

            for ((order, entityID) in Memory.orders) {
                if (Game.getObjectById<Creep>(entityID) == null) {
                    delete(Memory.orders[order])
                }
            }
        }
    } catch (e:Error) {
        console.log(e.message)
    }
}
