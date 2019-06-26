package entities.creeps

import screeps.api.structures.StructureStorage
import screeps.api.values

fun StructureStorage.capacity():Int { return this.store.values.sum() }
fun StructureStorage.isFull():Boolean { return this.capacity() == this.storeCapacity }