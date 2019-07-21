package entities.structures

import screeps.api.EnergyContainer
import screeps.api.Store
import screeps.api.structures.StructureNuker
import screeps.api.structures.StructurePowerSpawn
import screeps.api.values
import screeps.utils.lazyPerTick

inline fun Store.capacity():Int { return this.store.values.sum() }
inline fun Store.isFull():Boolean { return this.capacity() == this.storeCapacity }

inline fun EnergyContainer.isFull():Boolean { return this.energy == this.energyCapacity }
inline fun EnergyContainer.capacityCoef():Float { return this.energy.toFloat() / this.energyCapacity }
inline fun EnergyContainer.halfCapacity():Int { return this.energyCapacity / 2 }
inline fun EnergyContainer.moreThanHalfCapacity():Boolean { return this.energy > this.energyCapacity / 2 }
inline fun EnergyContainer.lessThanHalfCapacity():Boolean { return this.energy < this.energyCapacity / 2 }

val StructurePowerSpawn.requiredPower:Int by lazyPerTick { powerCapacity - power }

inline fun StructureNuker.isFull():Boolean { return this.energy == this.energyCapacity && this.ghodium == this.ghodiumCapacity  }