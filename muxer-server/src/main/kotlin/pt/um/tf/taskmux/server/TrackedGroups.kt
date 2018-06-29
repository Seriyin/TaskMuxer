package pt.um.tf.taskmux.server;

import spread.SpreadGroup;

import java.util.*;

class TrackedGroups {
    private val knownM : TreeMap<String, SpreadGroup> = TreeMap()
    private val tracking : MutableMap<String, SpreadGroup> = mutableMapOf()
    val tracked: Set<String> get() = tracking.keys.toSet()
    val known: Set<String> get() = knownM.keys.toSet()

    fun registerKnown(privateGroup : SpreadGroup) {
        knownM[privateGroup.toString()] = privateGroup
    }

    fun registerKnown(privateGroups : Set<SpreadGroup>) {
        privateGroups.associateByTo(knownM, SpreadGroup::toString)
    }

    fun purgeKnown(left : String) {
        knownM -= left
    }

    fun purgeKnown(left : SpreadGroup) {
        knownM -= left.toString()
    }

    /**
     * Every Server will contain at minimum it's own group.
     * @return The minimum id private group.
     */
    fun getMinKnown() : SpreadGroup {
        return knownM.firstEntry().value
    }


    fun registerTracked(receiver : SpreadGroup) {
        tracking[receiver.toString()] = receiver
    }

    fun registerTracked(receiver : Set<SpreadGroup>) {
        receiver.associateByTo(tracking, SpreadGroup::toString)
    }

    fun purgeTracked(receiver : String) {
        tracking -= receiver
    }

    fun purgeTracked(receiver : SpreadGroup) {
        tracking -= receiver.toString()
    }

    fun getKnown(receiver : String) : SpreadGroup? {
        return knownM[receiver]
    }

    fun getTracked(receiver: String): SpreadGroup? {
        return tracking[receiver]
    }

    fun isNotEmptyTracked(): Boolean {
        return tracking.isNotEmpty()
    }
}
