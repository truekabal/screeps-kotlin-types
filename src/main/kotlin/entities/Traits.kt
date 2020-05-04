package entities

import screeps.api.StoreOwner
import screeps.api.ResourceConstant

fun StoreOwner.isFull():Boolean { return this.store.getFreeCapacity() == 0 }
fun StoreOwner.isNotFull():Boolean { return this.store.getFreeCapacity() > 0 }
fun StoreOwner.isEmpty():Boolean { return this.store.getUsedCapacity() == 0 }
fun StoreOwner.isNotEmpty():Boolean { return this.store.getUsedCapacity() > 0 }

inline fun StoreOwner.capacityCoef(resource:ResourceConstant):Float { return this.store.getUsedCapacity(resource)!!.toFloat() / this.store.getCapacity(resource)!! }
inline fun StoreOwner.halfCapacity():Int { return this.store.getCapacity()!! / 2 }
inline fun StoreOwner.moreThanHalfCapacity():Boolean { return this.store.getUsedCapacity() > this.store.getCapacity()!! / 2 }
inline fun StoreOwner.lessThanHalfCapacity():Boolean { return this.store.getUsedCapacity() <= this.store.getCapacity()!! / 2 }