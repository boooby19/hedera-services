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

package com.hedera.node.app.service.schedule;

import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.transaction.Query;
import com.hedera.hapi.node.transaction.Response;
import com.hedera.hapi.node.transaction.TransactionResponse;
import com.hedera.pbj.runtime.RpcMethodDefinition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ScheduleServiceDefinitionTest {

    @Test
    void checkBasePath() {
        Assertions.assertThat(ScheduleServiceDefinition.INSTANCE.basePath()).isEqualTo("proto.ScheduleService");
    }

    @Test
    void methodsDefined() {
        final var methods = ScheduleServiceDefinition.INSTANCE.methods();
        Assertions.assertThat(methods)
                .containsExactlyInAnyOrder(
                        new RpcMethodDefinition<>("createSchedule", Transaction.class, TransactionResponse.class),
                        new RpcMethodDefinition<>("signSchedule", Transaction.class, TransactionResponse.class),
                        new RpcMethodDefinition<>("deleteSchedule", Transaction.class, TransactionResponse.class),
                        new RpcMethodDefinition<>("getScheduleInfo", Query.class, Response.class));
    }
}
