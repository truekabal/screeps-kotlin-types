package entities.creeps

import screeps.api.Creep
import screeps.api.values

inline fun Creep.carryAmount():Int { return carry.values.sum() }
inline fun Creep.emptySpace():Int { return carryCapacity - carryAmount() }
inline fun Creep.isFull():Boolean { return carryAmount() == carryCapacity }
inline fun Creep.isEmpty():Boolean { return carryAmount() == 0 }
