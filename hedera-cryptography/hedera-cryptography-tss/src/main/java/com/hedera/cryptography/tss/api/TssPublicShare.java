/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.cryptography.tss.api;

import static java.util.Objects.requireNonNull;

import com.hedera.cryptography.pairings.signatures.api.PairingPublicKey;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A record that contains a share ID, and the corresponding public key.
 *
 * @param shareId the share ID
 * @param publicKey the public key
 */
public record TssPublicShare(@NonNull TssShareId shareId, @NonNull PairingPublicKey publicKey) {
    /**
     * Constructor
     *
     * @param shareId the share ID
     * @param publicKey the public key
     */
    public TssPublicShare {
        requireNonNull(shareId, "shareId must not be null");
        requireNonNull(shareId, "publicKey must not be null");
    }
}