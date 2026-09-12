package com.health.assistant.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256Hex(String input) {
        if (input == null) {
            return null;
        }
        return sha256Hex(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
