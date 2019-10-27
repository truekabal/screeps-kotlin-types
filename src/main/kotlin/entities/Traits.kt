package entities

import screeps.api.IStore
import screeps.api.ResourceConstant

fun IStore.isFull():Boolean { return this.store.getFreeCapacity() == 0 }
fun IStore.isNotFull():Boolean { return this.store.getFreeCapacity() > 0 }
fun IStore.isEmpty():Boolean { return this.store.getUsedCapacity() == 0 }
fun IStore.isNotEmpty():Boolean { return this.store.getUsedCapacity() > 0 }

inline fun IStore.capacityCoef(resource:ResourceConstant):Float { return this.store.getUsedCapacity(resource)!!.toFloat() / this.store.getCapacity(resource)!! }
inline fun IStore.halfCapacity():Int { return this.store.getCapacity() / 2 }
inline fun IStore.moreThanHalfCapacity():Boolean { return this.store.getUsedCapacity() > this.store.getCapacity() / 2 }
inline fun IStore.lessThanHalfCapacity():Boolean { return this.store.getUsedCapacity() <= this.store.getCapacity() / 2 }