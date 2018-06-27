package pt.um.tf.taskmux.commons.messaging

import java.net.URI

class URIMessage(private val uri : URI? = null) : CommonMessage {
    fun getURI() : URI {
        return uri as URI
    }
}
