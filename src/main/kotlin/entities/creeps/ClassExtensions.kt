package entities.creeps

import screeps.api.Creep
import screeps.api.values
import screeps.utils.lazyPerTick

val Creep.carryAmount:Int by lazyPerTick { carry.values.sum() }
val Creep.emptySpace:Int by lazyPerTick { carryCapacity - carryAmount }
val Creep.isFull:Boolean by lazyPerTick { carryAmount == carryCapacity }
val Creep.isEmpty:Boolean by lazyPerTick { carryAmount == 0 }