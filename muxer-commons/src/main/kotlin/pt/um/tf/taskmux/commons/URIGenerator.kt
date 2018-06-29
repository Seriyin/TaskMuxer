package pt.um.tf.taskmux.commons;

import mu.KLogging
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

class URIGenerator (seed: String? = null) {
    private val sR : SecureRandom = SecureRandom(seed?.toByteArray(StandardCharsets.UTF_8))

    /**
     * Returns a Base64 Encoding of the SHA-256 hash of the provided data string.
     */
    private fun getSHA256(sdata : String) : String {
        val sb = StringBuilder()
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sdata.toByteArray(StandardCharsets.UTF_8))
        val byteData = md.digest()
        sb.append(Base64.getEncoder().encodeToString(byteData))
        return sb.toString()
    }

    private fun generateRandomUrlPostfix() : String {
        val b = ByteArray(512)
        sR.nextBytes(b)
        return String(b, StandardCharsets.UTF_8)
    }

    fun generateURLPostFix() : String {
        return getSHA256(generateRandomUrlPostfix())
    }
}
