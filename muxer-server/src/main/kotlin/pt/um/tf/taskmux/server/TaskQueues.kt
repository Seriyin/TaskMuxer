package pt.um.tf.taskmux.server

import pt.um.tf.taskmux.commons.IndexedDeque
import pt.um.tf.taskmux.commons.error.DuplicateException
import pt.um.tf.taskmux.commons.error.NoAssignableTasksException
import pt.um.tf.taskmux.commons.task.Result
import pt.um.tf.taskmux.commons.task.Task
import pt.um.tf.taskmux.server.messaging.OutboundMessage
import java.net.URI


class TaskQueues {
    private val inbound : IndexedDeque<Task<out Any>> = IndexedDeque()
    private val outbound : MutableMap<String, MutableMap<URI, Task<out Any>>> = mutableMapOf()
    private val iteratorMap : MutableMap<String, Iterator<Map.Entry<String, MutableMap<URI, Task<out Any>>>>> = mutableMapOf()

    fun addToFrontInbound(task : Task<out Any>) {
        inbound.addFirst(task)
    }

    fun addAllToBackInbound(tasks : Set<Task<out Any>>) {
        tasks.forEach(inbound::addLast)
    }

    fun clearTasks(toClear : String) {
        outbound[toClear]?.clear()
        outbound.remove(toClear)
    }

    fun replaceAfterCompleted(sender : String,
                              tasks : Set<Task<out Any>>) {
        outbound[sender] = tasks.associateByTo(mutableMapOf(),
                                               Task<out Any>::uri)
    }

    fun replaceAfterGet(inSet : Set<Task<out Any>>, sender : String, count : Int) {
        outbound[sender] = inSet.associateByTo(mutableMapOf(), Task<out Any>::uri)
        repeat(if (count > inbound.size) inbound.size else count) {
            inbound.removeFirst()
        }
    }

    fun sendToOutbound(sender : String) : Result<Task<out Any>> {
        if (!outbound.containsKey(sender)) {
            outbound[sender] = mutableMapOf()
        }
        return if(inboundIsEmpty()) {
            TaskResult(false, null, NoAssignableTasksException())
        }
        else {
            val inSet = inbound.removeFirst()
            val out = outbound[sender]
            out?.put(inSet.uri, inSet)
            if (out != null) {
                TaskResult(true, inSet, null)
            } else {
                //Should never happen
                TaskResult(false, null, DuplicateException())
            }
        }
    }

    fun backToInbound(mem : String) : Set<Task<out Any>> {
        val out = outbound[mem]
        val backToInbound = setOf(*out?.values?.toTypedArray() ?: arrayOf())
        backToInbound.forEach {
            inbound.addFirst(it)
        }
        outbound.remove(mem)
        return backToInbound
    }

    fun sendToInbound(task : Task<out Any>) = inbound.addLast(task)

    fun AreBothEmpty() : Boolean = inbound.isEmpty() && outbound.isEmpty()

    fun inboundIsEmpty() : Boolean = inbound.isEmpty()

    fun outboundIsEmpty() : Boolean = outbound.isEmpty()

    fun getOutboundIterator(receiver : String) : Iterator<Map.Entry<String, Map<URI, Task<out Any>>>>? {
        return if(!iteratorMap.containsKey(receiver)) {
            val it = outbound.entries.iterator()
            iteratorMap[receiver] = it
            it
        }
        else {
            iteratorMap[receiver]
        }
    }

    fun purgeOutbound(sender : String, url : URI) : Set<Task<out Any>> {
        return if (outbound.containsKey(sender)) {
            val out = outbound[sender]
            if (out?.remove(url) != null) {
                setOf(*out.values.toTypedArray())
            } else {
              emptySet()
            }
        } else {
          emptySet()
        }
    }

    fun getInbound() : Set<Task<out Any>> = inbound.toSet()

    fun getOutbound(sender : String) : Set<Task<out Any>> {
        return outbound[sender]?.values?.toSet() ?: emptySet()
    }

    fun wipe() {
        inbound.clear()
        outbound.clear()
    }

    fun purgeIterator(receiver: String) {
        iteratorMap.remove(receiver)
    }
}
