package main.kotlin.entities.structures

import screeps.api.*
import screeps.api.structures.StructureTower

class DelegateTower(private val tower: StructureTower): StructureBase(tower) {
    override fun tick() {
        if (tower.store.getUsedCapacity(RESOURCE_ENERGY)!! == 0) return

        val hostiles = tower.room.find(FIND_HOSTILE_CREEPS)
        if (hostiles.isNotEmpty()) {
            tower.attack(hostiles[0])
            return
        }

        val damagedCreeps = tower.room.find(FIND_MY_CREEPS, options { filter = {it.hits < it.hitsMax} })
        if (damagedCreeps.isNotEmpty()) {
            damagedCreeps.sort { a, b -> (a.hitsMax - a.hits) - (b.hitsMax - b.hits) }
            tower.heal(damagedCreeps.random())
            return
        }

        if (tower.store.getUsedCapacity(RESOURCE_ENERGY)!! < tower.store.getCapacity(RESOURCE_ENERGY)!! / 2) return

        val structures = tower.room.find(FIND_STRUCTURES, options { filter = { (it.unsafeCast<Decaying>()).ticksToDecay > 0 && it.hits < 2500000 && it.hits < it.hitsMax } })
        if (structures.isNotEmpty()) {
            structures.sort {a,b -> a.hits - b.hits }
            tower.repair(structures[0])
            return
        }
    }
}
