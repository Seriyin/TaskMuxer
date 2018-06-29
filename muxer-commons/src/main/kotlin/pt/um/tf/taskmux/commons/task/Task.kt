package pt.um.tf.taskmux.commons.task;

import mu.KLogging
import pt.um.tf.taskmux.commons.URIGenerator
import java.io.Serializable
import java.net.URI

abstract class Task<T> (u : URIGenerator = URIGenerator()) : Serializable {//implements CatalystSerializable {
    companion object : KLogging()
    private var _uri = URI("tcp",
            null,
            "localhost",
            4803,
            "/dummytask${u.generateURLPostFix()}",
            null,
            null)
    var uri get() = _uri
    set(value) {
       _uri = value
    }

    abstract fun start() : T
    abstract fun completed() : Boolean
    abstract fun cancel()

}
