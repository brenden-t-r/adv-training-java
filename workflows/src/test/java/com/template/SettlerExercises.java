package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.template.flows.*;
import com.template.states.IOUState;
import com.template.states.IOUToken;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class SettlerExercises {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode fxOracle = network.createNode(
            new CordaX500Name("ExchangeRateOracleService", "New York", "US")
    );
    private final StartedMockNode settlerOracle = network.createNode(
            new CordaX500Name("SettlerOracleService", "New York", "US")
    );

    public SettlerExercises() {}

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    /**
     * TODO: Implement the [IOUNovateFlow].
     * @see IOUNovateFlow
     * Hint:
     * - In this flow, we will take a previously created IOU between Party A and Party B with
     * an amount of IOUTokens as the owed amount, and we'll novate it in terms of US Dollars.
     * We will use this flow later as a subflow in the [IOUSettleFlow].
     *
     * - First, we need to get the notary and Exchange Rate Oracle [Party]
     * identity using the [serviceHub].
     * - Then, we build the transaction:
     * -- Use the existing IOU as input state.
     * -- Add a new IOUState as output that has the amount field replaced with the amount
     * in terms of USD. Use the [withNewAmount] helper method of IOUState and the
     * [FiatCurrency.Companion.getInstance] method to get the Currency code.
     * -- Add the Novate command and be sure to add the Oracle as a required signer.
     * - Then, verify and sign the transaction.
     * - Finally, subflow the [ExchangeRateOracleFlow] and return the result.
     */
    @Test
    public void implementIOUNovateFlow() throws ExecutionException, InterruptedException {
        // Create an IOU between party A and B using IOUToken
        IOUState state = new IOUState(
                new Amount<TokenType>(100L, new IOUToken("CUSTOM_TOKEN", 0)),
                a.getInfo().getLegalIdentities().get(0),
                b.getInfo().getLegalIdentities().get(0));

        Future<SignedTransaction> issueFuture = a.startFlow(new IOUIssueFlow.InitiatorFlow(state));
        network.runNetwork();
        SignedTransaction signedTx = issueFuture.get();

        // Novate the IOU to be in terms of USD TokenType
        StateAndRef<IOUState> stateAndRef = vaultQuery(((IOUState)signedTx.getTx().getOutputStates().get(0)).getLinearId());
        Future<SignedTransaction> future = a.startFlow(new IOUNovateFlow.Initiator(stateAndRef, "USD"));
        network.runNetwork();
        SignedTransaction stx = future.get();

        assertEquals(1, signedTx.getTx().getOutputs().size());
        IOUState output = (IOUState) stx.getTx().getOutputStates().get(0);
        assertEquals(150, output.getAmount().getQuantity());
        assertEquals("USD", output.getAmount().getToken().getTokenIdentifier());
        assertEquals(false, output.getSettled().booleanValue());
        assertEquals(2, stx.getSigs().size());
    }

    /**
     * TODO: Implement the [OffLedgerPaymentFlow].
     * @see OffLedgerPaymentFlow
     * @see OffLedgerPaymentRailService
     * Hint:
     * In this simple flow, we will use the [OffLedgerPaymentRailService] to
     * initiate the off ledger payment and return the resulting transaction ID.
     *
     * - First, use the [ServiceHub] to get the OffledgerPaymentRail [CordaService]
     * - Then, call the [makePayment] method with the off ledger account number and
     * amount of USD to send and return the result.
     */
    @Test
    public void implementOffLedgerPaymentFlow() throws ExecutionException, InterruptedException {
        Future<String> issueFuture = a.startFlow(new OffLedgerPaymentFlow("ABCD1234", 300.0));
        network.runNetwork();
        String transactionId = issueFuture.get();

        assertEquals("TEST_TRANSACTION_ID_1234", transactionId);
    }

    /**
     * TODO: Implement the [IOUVerifySettlementFlow].
     * @see IOUVerifySettlementFlow
     * Hint:
     * In this flow, we will use our Settler Oracle to verify that the off ledger
     * payment was made and the correct amount was paid in US Dollars.
     *
     * - First, we need to get the notary and Settler Oracle [Party]
     * identity using the [serviceHub].
     * - Then, we build the transaction:
     * -- Use the existing IOU as input state.
     * -- Add a new IOUState as output that has the IOU marked as settled
     * using the [withSettled] helper method of IOUState.
     * -- Add the Settle command and be sure to add the Oracle as a required signer.
     * - Then, verify and sign the transaction.
     * - Finally, we need to get the Settler Oracle to verify the transaction and
     * sign the transaction.
     * -- Subflow the [SignOffLedgerPayment] flow. The Settler Oracle will use
     * the SettlerOracleService to verify the off ledger payment using the
     * transaction id. The flow will return the Oracle's signature.
     * -- Add the signature to the SignedTransaction and return this.
     */
    @Test
    public void implementIOUVerifySettlementFlow() throws ExecutionException, InterruptedException {
        IOUState state = new IOUState(
                new Amount<TokenType>(100L, new IOUToken("CUSTOM_TOKEN", 0)),
                a.getInfo().getLegalIdentities().get(0),
                b.getInfo().getLegalIdentities().get(0));

        Future<SignedTransaction> issueFuture = a.startFlow(new IOUIssueFlow.InitiatorFlow(state));
        network.runNetwork();
        SignedTransaction signedTx = issueFuture.get();

        assertEquals(1, signedTx.getTx().getOutputs().size());

        StateAndRef<IOUState> stateAndRef = vaultQuery(((IOUState)signedTx.getTx().getOutputStates().get(0)).getLinearId());
        Future<SignedTransaction> future = a.startFlow(new IOUSettleFlow.Initiator(stateAndRef, "ABCD1234"));
        network.runNetwork();
        SignedTransaction stx = future.get();

        assertEquals(1, signedTx.getTx().getOutputs().size());
        IOUState output = (IOUState) stx.getTx().getOutputStates().get(0);
        assertEquals(150, output.getAmount().getQuantity());
        assertEquals("USD", output.getAmount().getToken().getTokenIdentifier());
        assertEquals(true, output.getSettled().booleanValue());
        assertEquals(4, stx.getSigs().size());
    }

    public StateAndRef<IOUState> vaultQuery(UniqueIdentifier linearId) {
        return a.getServices().getVaultService().queryBy(IOUState.class,
                new QueryCriteria.LinearStateQueryCriteria(
                        null, ImmutableList.of(linearId), Vault.StateStatus.UNCONSUMED, null))
                .getStates().get(0);
    }
}
