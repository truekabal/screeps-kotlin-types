package entities.creeps

import screeps.api.structures.StructureContainer
import screeps.api.values

fun StructureContainer.capacity():Int { return this.store.values.sum() }
fun StructureContainer.isFull():Boolean { return this.capacity() == this.storeCapacity }