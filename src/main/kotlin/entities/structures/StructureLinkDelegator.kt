package entities.structures

import main.kotlin.memory.*
import screeps.api.*
import screeps.api.structures.StructureLink
import kotlin.math.ceil
import kotlin.math.min

const val LINK_UNLOCK_ROOM_LEVEL:Int = 5
const val LINK_ROLE_MANAGE_PERIOD:Int = 25
const val MIN_ENERGY_TO_TRANSFER:Int = 100

fun roomCanHaveLinks(room:Room):Boolean {
    return room.controller != null && room.controller.my && room.controller.level > 0 && CONTROLLER_STRUCTURES[STRUCTURE_LINK]!![room.controller.level]!! > 0
}

fun manageStructureLinkRoles() {
    if (Memory.timer % LINK_ROLE_MANAGE_PERIOD != 0) return
    val prevUsedCPU = Game.cpu.getUsed()
    for ((roomName, room) in Game.rooms) {
        if (!roomCanHaveLinks(room)) {
            continue
        }


        val links = room.find(FIND_MY_STRUCTURES, opts = options {filter = { it.structureType == STRUCTURE_LINK }}).unsafeCast<Array<StructureLink>>()
        if (links.size < 2) {
            room.memory.links = js("{}")
            continue
        }

        val memory = room.memory.links
        //------------     sources     -------------------
        val sources = room.find(FIND_SOURCES)
        if (sources.isNotEmpty()) {
            for (source in sources) {
                findLinksFor(source, memory.sources[source.id], links) { memory.sources[source.id] = it }
            }
        }

        //------------     storage     -------------------
        findLinksFor(room.storage, memory.storage, links) { memory.storage = it }

        //------------     terminal     -------------------
        findLinksFor(room.terminal, memory.terminal, links) { memory.terminal = it }

        //------------     labs     -------------------
        val labs = room.find(FIND_MY_STRUCTURES, opts = options {filter = { it.structureType == STRUCTURE_LAB }})
        if (labs.isNotEmpty()) {
            for (lab in labs) {
                val labsLinkIsSet = findLinksFor(lab, memory.labs, links) {memory.labs = it}

                // if we have link per any lab - we have link for them all
                if (labsLinkIsSet) {
                    break
                }
            }
        }

        //------------     controller     -------------------
        findLinksFor(room.controller, memory.controller, links) { memory.controller = it }
    }

    /*
    val usedCPU = Game.cpu.getUsed()
    if (usedCPU > prevUsedCPU)
    {
        console.log("manageStructureLinkRoles execution cost: ${usedCPU - prevUsedCPU}")
    }
    */
}

fun findLinksFor(obj:RoomObject?, linkID:String?, links:Array<StructureLink>, callback: (linkID:String) -> Unit):Boolean {
    if (obj == null) {
        return false
    }
    return findLinksFor(obj, linkID, links, callback)
}

fun findLinksFor(obj:RoomObject, linkID:String?, links:Array<StructureLink>, callback: (linkID:String) -> Unit):Boolean {

    if (!linkID.isNullOrEmpty() && Game.getObjectById<StructureLink>(linkID) != null) {
        return false
    }

    val pos = obj.pos
    val linksInRange = pos.findInRange(links, 4)
    if (linksInRange.isEmpty()) {
        return false
    }

    if (linksInRange.size > 1) {
        links.sortBy { pos.getRangeTo(it) }
    }

    callback(linksInRange.first().id)
    return true
}

fun transferEnergyByLinks(room:Room) {
    if (!roomCanHaveLinks(room)) {
        return
    }

    var availableLinks:List<StructureLink> = emptyList()

    val memory = room.memory.links
    var link:StructureLink?
    for ((sourceID, linkID) in memory.sources) {
        if (linkID != null) {
            link = Game.getObjectById(linkID)
            if (link != null && link.cooldown == 0 && link.store.getUsedCapacity(RESOURCE_ENERGY)!! > MIN_ENERGY_TO_TRANSFER) {
                availableLinks = availableLinks.plus(link)
            }
        }
    }

    if (memory.storage != null && availableLinks.isEmpty()) {
        link = Game.getObjectById(memory.storage)
        if (link != null && link.cooldown == 0 && link.store.getUsedCapacity(RESOURCE_ENERGY)!! > link.store.getCapacity(RESOURCE_ENERGY)!! / 2) {
            availableLinks = availableLinks.plus(link)
        }
    }

    if (availableLinks.isEmpty()) {
        return
    }

    val linksToReceive = arrayOf(memory.labs, memory.controller, memory.terminal, memory.storage).map { if (it != null) Game.getObjectById<StructureLink>(it) else null }
    for (target in linksToReceive) {
        if (target == null) {
            continue
        }

        if (target.store.getUsedCapacity(RESOURCE_ENERGY)!! < target.store.getCapacity(RESOURCE_ENERGY)!! - MIN_ENERGY_TO_TRANSFER / 2) {
            var energyNeed = target.store.getFreeCapacity(RESOURCE_ENERGY)!!
            var energyReceived = 0
            var lastUsedIndex:Int = 0

            for ((i, source) in availableLinks.withIndex()) {
                if (target == source) {
                    continue
                }
                val energy = min(energyNeed, source.store.getUsedCapacity(RESOURCE_ENERGY)!!)
                val result = source.transferEnergy(target, energy)
                if (result == OK) {
                    energyReceived += (energy - ceil(energy * LINK_LOSS_RATIO)).toInt()
                    lastUsedIndex = i + 1
                    energyNeed -= energy
                    if (energyNeed <= 35) {
                        RoomVisual(target.room.name).text("-$energy", source.pos)
                        break
                    }
                }

            }

            if (lastUsedIndex > 0) {
                RoomVisual(target.room.name).text("+$energyReceived", target.pos)
                availableLinks = availableLinks.drop(lastUsedIndex)
            }

            if (availableLinks.isEmpty()) {
                return
            }
        }
    }

}