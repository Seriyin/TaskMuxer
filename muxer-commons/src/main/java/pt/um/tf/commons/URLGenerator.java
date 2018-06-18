package pt.um.tf.commons;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class URLGenerator {
    private SecureRandom secureRandom;

    public URLGenerator (String seed) {
        secureRandom = new SecureRandom(seed.getBytes(StandardCharsets.UTF_8));
    }

    public URLGenerator() {
        secureRandom = new SecureRandom();
    }

    /**
     * Returns a Base64 Encoding of the SHA-256 hash of the provided data string.
     */
    public String getSHA256(String data) {
        var sb = new StringBuilder();
        try {
            var md = MessageDigest.getInstance("SHA-256");
            md.update(data.getBytes(StandardCharsets.UTF_8));
            var byteData = md.digest();
            sb.append(Base64.getEncoder().encodeToString(byteData));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String generateRandomUrlPostfix() {
        var b = new byte[512];
        secureRandom.nextBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public String generateURLPostFix() {
        return getSHA256(generateRandomUrlPostfix());
    }
}
