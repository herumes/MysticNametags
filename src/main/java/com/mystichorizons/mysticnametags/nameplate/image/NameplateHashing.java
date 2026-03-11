package com.mystichorizons.mysticnametags.nameplate.image;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Small utility for stable content hashing.
 */
public final class NameplateHashing {

    private NameplateHashing() {
    }

    @Nonnull
    public static String sha256Short(@Nonnull String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}