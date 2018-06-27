package pt.um.tf.taskmux.commons.task;

import mu.KLogging
import pt.um.tf.taskmux.commons.URIGenerator
import java.io.Serializable
import java.net.URI

abstract class Task<T> (id : String? = null) : Serializable {//implements CatalystSerializable {
    companion object : KLogging()
    private var uri : URI? = generateURI(id)

    private fun generateURI(id : String?) : URI? {
        val uri : URI?
        if(id == null) {
            val u = URIGenerator(id)
            uri = URI("tcp",
                    null,
                    "localhost",
                    4803,
                    "/dummytask${u.generateURLPostFix()}",
                    null,
                    null)
        }
        else {
            uri = null
        }
        return uri
    }

    fun getURI() : URI? {
        return uri
    }

    internal fun setURI(uri : URI?) {
        this.uri = uri
    }


    abstract fun start() : T
    abstract fun completed() : Boolean
    abstract fun cancel()

}
