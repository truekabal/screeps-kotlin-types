package main.kotlin

import main.kotlin.memory.orders
import main.kotlin.memory.targetID
import main.kotlin.memory.timer
import screeps.api.*
import screeps.api.Game.creeps
import screeps.utils.unsafe.delete

val GC_PERIOD:Int = 10

class GC {
    fun run() {
        try {
            if (Memory.timer % GC_PERIOD == 0) {
                gcCreeps()
                gcOrders()
            }
        } catch (e:Error) {
            console.log(e.message)
        }
    }

    private fun gcCreeps() {
        for ((creepName, _) in Memory.creeps) {
            if (creeps[creepName] == null) {
                delete(Memory.creeps[creepName])
            }
        }
    }

    private fun gcOrders() {
        for ((order, entityID) in Memory.orders) {
            val creep:Creep? = Game.getObjectById<Creep>(entityID)
            if (creep == null || creep.memory.targetID != order)
            {
                delete(Memory.orders[order])
            }
        }
    }
}