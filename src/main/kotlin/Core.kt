package main.kotlin

import entities.structures.manageStructureLinkRoles
import entities.structures.transferEnergyByLinks
import main.kotlin.entities.creeps.tick
import main.kotlin.entities.structures.tick
import main.kotlin.memory.timer
import screeps.api.*

class Core {

    //---------------------------------------------------------------------------------------
    fun tick() {
        tickTimer()
        memoryActions()
        lazyActions()
        preActions()
        mainActions()
        postActions()
    }

    //---------------------------------------------------------------------------------------
    private fun tickTimer() {
        Memory.timer = (Memory.timer + 1) % 10000
    }

    //---------------------------------------------------------------------------------------
    private fun lazyActions() {
        manageStructureLinkRoles()
    }

    //---------------------------------------------------------------------------------------
    private fun memoryActions() {

    }

    //---------------------------------------------------------------------------------------
    private fun preActions() {

    }

    //---------------------------------------------------------------------------------------
    private fun mainActions() {
        // update spawns, buildings, creeps

        for ((_, room) in Game.rooms) {
            transferEnergyByLinks(room)
        }

        for ((_, creep) in Game.creeps) {
            creep.tick()
        }

        for ((strName, structure) in Game.structures) {
            structure.tick()
        }
    }

    //---------------------------------------------------------------------------------------
    private fun postActions() {

    }

}

