package main.kotlin

import main.kotlin.entities.creeps.tick
import main.kotlin.entities.structures.tick
import main.kotlin.memory.orders
import main.kotlin.memory.timer
import screeps.api.*
import screeps.utils.unsafe.jsObject

//---------------------------------------------------------------------------------------
fun tickTimer() {
    Memory.timer += 1
    if (Memory.timer > 999) {
        // 1k ticks is fairly enough for any cycle :)
        Memory.timer = 0
    }
}

//---------------------------------------------------------------------------------------
fun garbageCollection() {
    GC()
}

//---------------------------------------------------------------------------------------
// not important and seldom running actions and calculations
fun lazyActions() {
    // recalc paths
    // renew creeps memory
    // e.t.c.
}

//---------------------------------------------------------------------------------------
fun memoryActions() {
    js("if (!Memory.orders) { Memory.orders = {}; }") // init orders
}

//---------------------------------------------------------------------------------------
fun preActions() {

}

//---------------------------------------------------------------------------------------
fun mainActions() {
    // update spawns, buildings, creeps

    for ((creepName, creep) in Game.creeps) {
        creep.tick()
    }

//    for ((spawnName, spawn) in Game.spawns) {
//        spawn.tick()
//    }

    for ((structureName, structure) in Game.structures) {
        structure.tick()
    }
}

//---------------------------------------------------------------------------------------
fun postActions() {

}

