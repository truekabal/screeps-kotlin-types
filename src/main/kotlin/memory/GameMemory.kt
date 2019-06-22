package main.kotlin.memory

import main.kotlin.CREEP_ROLE
import main.kotlin.CREEP_STATE
import screeps.api.*
import screeps.utils.containsKey
import screeps.utils.mutableRecordOf
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject

// orders
external interface OrdersMemory: MutableRecord<String, String>, MemoryMarker
var Memory.orders: OrdersMemory
    get() {
        return this["orders"].unsafeCast<OrdersMemory>()
    }
    set(value) {
        this["orders"] = value
    }

var Memory.timer: Int by memory { 0 }

// creeps
//TODO: remove this ugly flags
var CreepMemory.upgrading: Boolean by memory { false }
var CreepMemory.building: Boolean by memory { false }
var CreepMemory.pause: Int by memory { 0 }

var CreepMemory.targetID: String? by memory()
var CreepMemory.state:Int by memory { CREEP_STATE.UNDEFINED.ordinal }
var CreepMemory.role:Int by memory { CREEP_ROLE.UNDEFINED.ordinal }

// creep claim
var CreepMemory.room:String by memory { "" }

// creep harvester
var CreepMemory.sourceID: String by memory { "" }

// orders
 // initializes in seldom actions

