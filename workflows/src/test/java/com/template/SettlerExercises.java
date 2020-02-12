package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.template.flows.ExchangeRateOracle;
import com.template.flows.IOUIssueFlow;
import com.template.flows.IOUSettleFlow;
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

    public SettlerExercises() {
        a.registerInitiatedFlow(ExchangeRateOracle.QueryHandler.class);
        b.registerInitiatedFlow(ExchangeRateOracle.QueryHandler.class);
        a.registerInitiatedFlow(ExchangeRateOracle.SignHandler.class);
        b.registerInitiatedFlow(ExchangeRateOracle.SignHandler.class);
        //a.registerInitiatedFlow(IOUSettleFlow.ResponderFlow.class);
        //b.registerInitiatedFlow(IOUSettleFlow.ResponderFlow.class);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void iouSettleFlow() throws ExecutionException, InterruptedException {

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
