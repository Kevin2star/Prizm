package com.prizm.domain;

import java.security.SecureRandom;

public final class JoinCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private final SecureRandom random;

    public JoinCodeGenerator() {
        this(new SecureRandom());
    }

    public JoinCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String nextCode() {
        StringBuilder builder = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
