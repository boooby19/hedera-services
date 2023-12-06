/*
 * Copyright (C) 2022-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.consensus;

import com.swirlds.common.system.events.EventConstants;
import com.swirlds.platform.state.signed.SignedState;
import java.util.function.LongUnaryOperator;

/**
 * Utilities for calculating round numbers
 */
public final class RoundCalculationUtils {
    private RoundCalculationUtils() {}

    /**
     * Returns the oldest round that is non-ancient. If no round is ancient, then it will return the first round ever
     *
     * @param roundsNonAncient
     * 		the number of non-ancient rounds
     * @param lastRoundDecided
     * 		the last round that has fame decided
     * @return oldest non-ancient number
     */
    public static long getOldestNonAncientRound(final int roundsNonAncient, final long lastRoundDecided) {
        // if we have N non-ancient consensus rounds, and the last one is M, then the oldest non-ancient round is
        // M-(N-1) which is equal to M-N+1
        // if no rounds are ancient yet, then the oldest non-ancient round is the first round ever
        return Math.max(lastRoundDecided - roundsNonAncient + 1, EventConstants.MINIMUM_ROUND_CREATED);
    }

    /**
     * Returns the minimum generation below which all events are ancient
     *
     * @param roundsNonAncient
     * 		the number of non-ancient rounds
     * @param lastRoundDecided
     * 		the last round that has fame decided
     * @param roundGenerationProvider
     * 		returns a round generation number for a given round number
     * @return minimum non-ancient generation
     */
    public static long getMinGenNonAncient(
            final int roundsNonAncient, final long lastRoundDecided, final LongUnaryOperator roundGenerationProvider) {
        // if a round generation is not defined for the oldest round, it will be EventConstants.GENERATION_UNDEFINED,
        // which is -1. in this case we will return FIRST_GENERATION, which is 0
        return Math.max(
                roundGenerationProvider.applyAsLong(getOldestNonAncientRound(roundsNonAncient, lastRoundDecided)),
                GraphGenerations.FIRST_GENERATION);
    }

    /**
     * Returns the minimum generation below which all events are ancient for the round of the signed state
     *
     * @param roundsNonAncient
     * 		the number of non-ancient rounds
     * @param signedState
     * 		the signed state that holds the minumum generation information
     * @return minimum non-ancient generation
     */
    public static long getMinGenNonAncient(final int roundsNonAncient, final SignedState signedState) {
        return getMinGenNonAncient(roundsNonAncient, signedState.getRound(), round -> signedState
                .getState()
                .getPlatformState()
                .getPlatformData()
                .getMinGen(round));
    }
}
