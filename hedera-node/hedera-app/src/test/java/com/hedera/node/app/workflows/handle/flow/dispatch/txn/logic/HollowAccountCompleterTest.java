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

package com.hedera.node.app.workflows.handle.flow.dispatch.txn.logic;

import static com.hedera.hapi.node.base.HederaFunctionality.ETHEREUM_TRANSACTION;
import static com.hedera.node.app.spi.key.KeyUtils.IMMUTABILITY_SENTINEL_KEY;
import static com.hedera.node.app.workflows.handle.flow.dispatch.child.logic.ChildRecordBuilderFactoryTest.asTxn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hedera.hapi.node.base.AccountID;
import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.Key;
import com.hedera.hapi.node.base.NftTransfer;
import com.hedera.hapi.node.base.SignatureMap;
import com.hedera.hapi.node.base.TokenID;
import com.hedera.hapi.node.base.TokenTransferList;
import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.base.TransactionID;
import com.hedera.hapi.node.contract.EthereumTransactionBody;
import com.hedera.hapi.node.state.token.Account;
import com.hedera.hapi.node.token.CryptoTransferTransactionBody;
import com.hedera.hapi.node.transaction.SignedTransaction;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.node.app.service.token.ReadableAccountStore;
import com.hedera.node.app.service.token.records.CryptoUpdateRecordBuilder;
import com.hedera.node.app.signature.KeyVerifier;
import com.hedera.node.app.signature.impl.SignatureVerificationImpl;
import com.hedera.node.app.spi.signatures.SignatureVerification;
import com.hedera.node.app.spi.workflows.HandleContext;
import com.hedera.node.app.workflows.TransactionInfo;
import com.hedera.node.app.workflows.dispatcher.ReadableStoreFactory;
import com.hedera.node.app.workflows.handle.flow.dispatch.Dispatch;
import com.hedera.node.app.workflows.handle.flow.txn.UserTransactionComponent;
import com.hedera.node.app.workflows.handle.flow.txn.logic.HollowAccountCompleter;
import com.hedera.node.app.workflows.handle.record.RecordListBuilder;
import com.hedera.node.app.workflows.handle.record.SingleTransactionRecordBuilderImpl;
import com.hedera.node.app.workflows.prehandle.PreHandleResult;
import com.hedera.node.config.testfixtures.HederaTestConfigBuilder;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.config.api.Configuration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HollowAccountCompleterTest {
    @Mock(strictness = LENIENT)
    private Dispatch dispatch;

    @Mock(strictness = LENIENT)
    private HandleContext handleContext;

    @Mock(strictness = LENIENT)
    private UserTransactionComponent userTxn;

    @Mock(strictness = LENIENT)
    private ReadableAccountStore accountStore;

    @Mock(strictness = LENIENT)
    private KeyVerifier keyVerifier;

    @Mock(strictness = LENIENT)
    private ReadableStoreFactory readableStoreFactory;

    @Mock(strictness = LENIENT)
    private PreHandleResult preHandleResult;

    @Mock(strictness = LENIENT)
    private SingleTransactionRecordBuilderImpl recordBuilder;

    private Configuration configuration = HederaTestConfigBuilder.createConfig();
    private static final Instant consensusTime = Instant.ofEpochSecond(1_234_567L);
    private static final RecordListBuilder recordListBuilder = new RecordListBuilder(consensusTime);
    private static final AccountID payerId =
            AccountID.newBuilder().accountNum(1_234L).build();
    private static final CryptoTransferTransactionBody transferBody = CryptoTransferTransactionBody.newBuilder()
            .tokenTransfers(TokenTransferList.newBuilder()
                    .token(TokenID.DEFAULT)
                    .nftTransfers(NftTransfer.newBuilder()
                            .receiverAccountID(AccountID.DEFAULT)
                            .senderAccountID(AccountID.DEFAULT)
                            .serialNumber(1)
                            .build())
                    .build())
            .build();
    private static final TransactionBody txBody = asTxn(transferBody, payerId, consensusTime);
    private static final SignedTransaction transaction = SignedTransaction.newBuilder()
            .bodyBytes(TransactionBody.PROTOBUF.toBytes(txBody))
            .build();
    private static final Bytes transactionBytes = SignedTransaction.PROTOBUF.toBytes(transaction);
    final TransactionInfo txnInfo = new TransactionInfo(
            Transaction.newBuilder().body(txBody).build(),
            txBody,
            SignatureMap.DEFAULT,
            transactionBytes,
            HederaFunctionality.CRYPTO_TRANSFER);

    @InjectMocks
    private HollowAccountCompleter hollowAccountCompleter;

    @BeforeEach
    void setUp() {
        when(dispatch.handleContext()).thenReturn(handleContext);
        when(dispatch.keyVerifier()).thenReturn(keyVerifier);
        when(handleContext.payer()).thenReturn(payerId);
        //        when(handleContext.configuration()).thenReturn(configuration);
        when(userTxn.recordListBuilder()).thenReturn(recordListBuilder);
        when(userTxn.configuration()).thenReturn(configuration);
        //        when(userTxn.txnInfo()).thenReturn(txnInfo);
        when(userTxn.readableStoreFactory()).thenReturn(readableStoreFactory);
        when(userTxn.readableStoreFactory().getStore(ReadableAccountStore.class))
                .thenReturn(accountStore);
        when(userTxn.preHandleResult()).thenReturn(preHandleResult);
        when(handleContext.dispatchPrecedingTransaction(any(), any(), any(), any()))
                .thenReturn(recordBuilder);
    }

    @Test
    void finalizeHollowAccountsNoHollowAccounts() {
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Collections.emptySet());

        hollowAccountCompleter.finalizeHollowAccounts(userTxn, dispatch);

        verifyNoInteractions(keyVerifier);
        verifyNoInteractions(handleContext);
    }

    @Test
    void doesntFinalizeHollowAccountsWithNoImmutabilitySentinelKey() {
        final var hollowAccount = Account.newBuilder()
                .accountId(AccountID.newBuilder().accountNum(1).build())
                .key(Key.DEFAULT)
                .alias(Bytes.wrap(new byte[] {1, 2, 3}))
                .build();
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Collections.singleton(hollowAccount));
        SignatureVerification verification =
                new SignatureVerificationImpl(Key.DEFAULT, Bytes.wrap(new byte[] {1, 2, 3}), true);
        when(keyVerifier.verificationFor(Bytes.wrap(new byte[] {1, 2, 3}))).thenReturn(verification);
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Set.of(hollowAccount));

        hollowAccountCompleter.finalizeHollowAccounts(userTxn, dispatch);

        verify(keyVerifier).verificationFor(Bytes.wrap(new byte[] {1, 2, 3}));
        verify(handleContext, never())
                .dispatchPrecedingTransaction(
                        eq(txBody), eq(CryptoUpdateRecordBuilder.class), isNull(), eq(AccountID.DEFAULT));
    }

    @Test
    void finalizeHollowAccountsWithHollowAccounts() {
        final var hollowAccount = Account.newBuilder()
                .accountId(AccountID.newBuilder().accountNum(1).build())
                .key(IMMUTABILITY_SENTINEL_KEY)
                .alias(Bytes.wrap(new byte[] {1, 2, 3}))
                .build();
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Collections.singleton(hollowAccount));
        SignatureVerification verification =
                new SignatureVerificationImpl(Key.DEFAULT, Bytes.wrap(new byte[] {1, 2, 3}), true);
        when(keyVerifier.verificationFor(Bytes.wrap(new byte[] {1, 2, 3}))).thenReturn(verification);
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Set.of(hollowAccount));

        hollowAccountCompleter.finalizeHollowAccounts(userTxn, dispatch);

        verify(keyVerifier).verificationFor(Bytes.wrap(new byte[] {1, 2, 3}));
        verify(handleContext).dispatchPrecedingTransaction(any(), any(), any(), any());
        verify(recordBuilder).accountID(AccountID.newBuilder().accountNum(1).build());
    }

    @Test
    void finalizeHollowAccountsWithEthereumTransaction() {
        when(userTxn.functionality()).thenReturn(ETHEREUM_TRANSACTION);

        final var hollowAccount = Account.newBuilder()
                .accountId(AccountID.newBuilder().accountNum(1).build())
                .key(IMMUTABILITY_SENTINEL_KEY)
                .alias(Bytes.wrap(new byte[] {1, 2, 3}))
                .build();
        final var txnBody = TransactionBody.newBuilder()
                .transactionID(TransactionID.newBuilder()
                        .accountID(AccountID.newBuilder().accountNum(1).build())
                        .build())
                .ethereumTransaction(EthereumTransactionBody.DEFAULT)
                .build();
        final TransactionInfo txnInfo = new TransactionInfo(
                Transaction.newBuilder().body(txnBody).build(),
                txnBody,
                SignatureMap.DEFAULT,
                transactionBytes,
                ETHEREUM_TRANSACTION);

        SignatureVerification ethVerification =
                new SignatureVerificationImpl(Key.DEFAULT, Bytes.wrap(new byte[] {1, 2, 3}), true);
        when(userTxn.preHandleResult().getHollowAccounts()).thenReturn(Set.of(hollowAccount));
        when(userTxn.readableStoreFactory().getStore(ReadableAccountStore.class))
                .thenReturn(accountStore);
        when(accountStore.getAccountIDByAlias(Bytes.wrap(new byte[] {1, 2, 3}))).thenReturn(AccountID.DEFAULT);
        when(accountStore.getAccountById(AccountID.DEFAULT)).thenReturn(hollowAccount);
        when(userTxn.configuration()).thenReturn(configuration);
        when(userTxn.recordListBuilder()).thenReturn(recordListBuilder);
        when(userTxn.txnInfo()).thenReturn(txnInfo);
        when(keyVerifier.verificationFor(Bytes.wrap(new byte[] {1, 2, 3}))).thenReturn(ethVerification);

        hollowAccountCompleter.finalizeHollowAccounts(userTxn, dispatch);

        verify(handleContext).dispatchPrecedingTransaction(any(), any(), any(), any());
        verify(recordBuilder).accountID(AccountID.newBuilder().accountNum(1).build());
    }
}