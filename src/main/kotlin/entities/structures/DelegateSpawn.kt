package main.kotlin.entities.structures

import main.kotlin.CREEP_ROLE
import main.kotlin.entities.creeps.getInitialCreepMemory
import main.kotlin.memory.role
import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.unsafe.delete
import screeps.utils.unsafe.jsObject
import kotlin.random.Random

val spawnParams: HashMap<Int, Array<out BodyPartConstant>> = hashMapOf(
    CREEP_ROLE.HARVESTER.ordinal  to arrayOf(WORK, WORK, WORK, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY),
    CREEP_ROLE.BUILDER.ordinal    to arrayOf(WORK, WORK, MOVE, MOVE, CARRY, CARRY),
    CREEP_ROLE.FIXER.ordinal      to arrayOf(WORK, WORK, MOVE, MOVE, CARRY, CARRY),
    CREEP_ROLE.CLAIM.ordinal      to arrayOf(CLAIM, CLAIM, MOVE),
    CREEP_ROLE.UPGRADER.ordinal   to arrayOf(WORK, WORK, WORK, WORK, WORK, WORK, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY),
    CREEP_ROLE.MANAGER.ordinal    to arrayOf(MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
)

class DelegateSpawn(private val spawn:StructureSpawn): StructureBase(spawn) {
    val roomToClaim: String? = null // "W46N33"
    inline fun StructureSpawn.energyAvailable(): Int = room.energyAvailable

    override fun tick() {
        if (spawn.spawning != null) {
            RoomVisual(spawn.room.name).text(
                    spawn.spawning.name,
                    (spawn.pos.x + 2).toDouble(),
                    spawn.pos.y.toDouble(),
                    jsObject { align = TEXT_ALIGN_LEFT; opacity = 0.5 }
            )
            return
        }

        if (tryRenewNearest()) return

        val storage = spawn.room.storage

        val creepsByRole = hashMapOf<Int, Int>(
                CREEP_ROLE.HARVESTER.ordinal  to 0,
                CREEP_ROLE.BUILDER.ordinal    to 0,
                CREEP_ROLE.FIXER.ordinal      to 0,
                CREEP_ROLE.CLAIM.ordinal      to 0,
                CREEP_ROLE.UPGRADER.ordinal   to 0,
                CREEP_ROLE.MANAGER.ordinal    to 0
        )

        val maxCreepsByRole = hashMapOf<Int, Int>(
                CREEP_ROLE.HARVESTER.ordinal  to 6,
                CREEP_ROLE.BUILDER.ordinal    to kotlin.math.min(kotlin.math.ceil(spawn.room.find(FIND_CONSTRUCTION_SITES).size.toDouble() / 4).toInt(), 3),
                CREEP_ROLE.FIXER.ordinal      to 1,
                CREEP_ROLE.CLAIM.ordinal      to (if (claimerNeed()) 1 else 0),
                CREEP_ROLE.UPGRADER.ordinal   to (if (storage != null && storage.store.energy > 900000) 3 else 1),
                CREEP_ROLE.MANAGER.ordinal    to (if (storage != null) 1 else 0)
        )

        //TODO: move to preactions
        for (creepData in Game.creeps) {
            val role  = creepData.component2().memory.role
            creepsByRole[role] = creepsByRole[role]!!.plus(1)
        }

        val roomLock:String = spawn.room.name + "lockEnergy"

        val roles = arrayOf(
                CREEP_ROLE.HARVESTER,
                CREEP_ROLE.MANAGER,
                CREEP_ROLE.UPGRADER,
                CREEP_ROLE.BUILDER,
                CREEP_ROLE.FIXER,
                CREEP_ROLE.CLAIM
        )
        for (role in roles) {
            val roleInt = role.ordinal
            if (creepsByRole[roleInt]!! < maxCreepsByRole[roleInt]!!) {
                Memory[roomLock] = true
                if (spawn.spawnCreep(
                                spawnParams[roleInt]!!.copyOf(),
                                "${spawn.room.name}_${role.toString().toLowerCase()}_${(Random.nextDouble() * 10000000).toInt()}",
                                options { memory = getInitialCreepMemory(spawn, roleInt) }
                    ) == OK)
                {
                    creepsByRole[roleInt] = creepsByRole[roleInt]!!.plus(1)
                    break
                }
            }
        }

        if (roles.any { role -> creepsByRole[role.ordinal]!! < maxCreepsByRole[role.ordinal]!! }) {
            return
        }

        delete(Memory[roomLock])
    }

    private fun tryRenewNearest():Boolean {
        if (Memory[spawn.room.name + "lockEnergy"] != null) return false

        val creeps = spawn.pos.findInRange(FIND_MY_CREEPS, 1)
        if (creeps.isNotEmpty()) {
            creeps.sortBy { creep: Creep -> creep.ticksToLive }
            for (creep in creeps) {
                val result : ScreepsReturnCode = spawn.renewCreep(creep)
                if (result == OK) {
                    creep.say("renew ${creep.ticksToLive}")
                    return true
                }
            }
        }

        return false
    }

    private fun claimerNeed():Boolean {
        if (roomToClaim == null) return false
        val room = Game.rooms[roomToClaim]
        return room == null || (room.controller != null && !room.controller.my)
    }

}