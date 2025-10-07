/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 Wren Security. All rights reserved.
 */
package org.wrensecurity.wrenam.authentication.modules.webauthn.impl;

import java.security.SecureRandom;
import org.wrensecurity.wrenam.authentication.modules.webauthn.WebAuthnChallengeProvider;

/**
 * Implementation of {@link WebAuthnChallengeProvider} that uses {@link SecureRandom} to generate
 * cryptographically strong random challenges for WebAuthn operations.
 */
public class SecureRandomChallengeProviderImpl implements WebAuthnChallengeProvider {

    private static final int DEFAULT_CHALLENGE_BYTE_LENGTH = 32;

    private final int challengeByteLength;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new {@code SecureRandomChallengeProviderImpl} with the default
     * challenge length of 32 bytes.
     */
    public SecureRandomChallengeProviderImpl() {
        this(DEFAULT_CHALLENGE_BYTE_LENGTH);
    }

    /**
     * Create a new {@code SecureRandomChallengeProviderImpl} with the specified challenge byte length.
     *
     * @param challengeByteLength the number of bytes in each generated challenge; must be at least 16 bytes
     * @throws IllegalArgumentException if {@code challengeByteLength} is less than 16
     */
    public SecureRandomChallengeProviderImpl(int challengeByteLength) {
        if (challengeByteLength < 16) {
            throw new IllegalArgumentException("challengeByteLength must be at least 16");
        }
        this.challengeByteLength = challengeByteLength;
    }

    @Override
    public byte[] generateChallenge() {
        byte[] challengeBytes = new byte[challengeByteLength];
        secureRandom.nextBytes(challengeBytes);
        return challengeBytes;
    }

}
