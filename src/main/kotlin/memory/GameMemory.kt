package main.kotlin.memory

import main.kotlin.CREEP_ROLE
import main.kotlin.CREEP_STATE
import screeps.api.*
import screeps.utils.memory.memory

var Memory.timer: Int by memory { 0 }

// ----------------------------------------------------------------------------------------------
// orders
// ----------------------------------------------------------------------------------------------
var Memory.orders: MutableRecord<String, String> by memory { js("{}")  }

// ----------------------------------------------------------------------------------------------
// room memory
// ----------------------------------------------------------------------------------------------
external interface RoomLinksMemory: MemoryMarker
var RoomLinksMemory.sources:MutableRecord<String, String?> by memory { js("{}")  }
var RoomLinksMemory.labs:String? by memory()
var RoomLinksMemory.storage:String? by memory()
var RoomLinksMemory.terminal:String? by memory()
var RoomLinksMemory.controller:String? by memory()

var RoomMemory.links:RoomLinksMemory by memory { js("{}") }

// creeps
//TODO: remove this ugly flags
var CreepMemory.upgrading: Boolean by memory { false }
var CreepMemory.building: Boolean by memory { false }

var CreepMemory.targetID: String? by memory()
var CreepMemory.nextTargetID: String? by memory()

var CreepMemory.resource: ResourceConstant? by memory()
var CreepMemory.resourceAmount: Int by memory { 0 }

var CreepMemory.state:Int by memory { CREEP_STATE.UNDEFINED.ordinal }
var CreepMemory.role:Int by memory { CREEP_ROLE.UNDEFINED.ordinal }

// creep claim
var CreepMemory.room:String by memory {""}

// creep harvester
var CreepMemory.sourceID: String? by memory()

// orders
 // initializes in seldom actions

