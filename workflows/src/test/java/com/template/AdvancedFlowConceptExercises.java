package com.template;

import com.google.common.collect.ImmutableList;
import com.template.contracts.IOUContract;
import com.template.flows.ExchangeRateOracleFlow;
import com.template.flows.Responder;
import com.template.states.IOUToken;
import com.template.states.IOUTokenState;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static groovy.util.GroovyTestCase.assertEquals;

public class AdvancedFlowConceptExercises {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode(
            new CordaX500Name("ExchangeRateOracleService", "New York", "US")
    );

    public AdvancedFlowConceptExercises() {
        //a.registerInitiatedFlow(QueryHandler.class);
        //b.registerInitiatedFlow(QueryHandler.class);
        //a.registerInitiatedFlow(SignHandler.class);
        //b.registerInitiatedFlow(SignHandler.class);
    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    /**
     * TODO: Implement the [createFilteredTransaction] method in [ExchangeRateOracleFlow].
     * Hint:
     * - Use the [buildFilteredTransaction] from the [SignedTransaction] argument passed into the method.
     * - [buildFilteredTransaction] takes a [Predicate] lambda argument.
     * -- Within the [Predicate] a simple when/is block allows you to filter to only the transaction components
     * that you want to add.
     * -- In our case, we only want the Oracle to see Command of which the Oracle is a required signer of.
     * -- Example usage:
     *      Predicate {
     *          when (it) {
     *              is Command<*> -> // boolean expression
     *              else -> false
     *          }
     *      }
     */
    @Test
    public void filteredTransaction() {
        Party notary = a.getServices().getNetworkMapCache()
                .getNotaryIdentities().get(0);
        TransactionBuilder builder = new TransactionBuilder(notary);
        IOUTokenState output = new IOUTokenState(
                new Amount(3, new IOUToken("CUSTOM_TOKEN",2)),
                a.getServices().getMyInfo().getLegalIdentities().get(0),
                b.getServices().getMyInfo().getLegalIdentities().get(0));
        builder.addCommand(
                new IOUContract.Commands.Exchange("USD", 1.25),
                ImmutableList.of(c.getInfo().getLegalIdentities().get(0).getOwningKey(),
                        a.getInfo().getLegalIdentities().get(0).getOwningKey()));
        builder.addCommand(
                new IOUContract.Commands.Exchange("USD", 1.75),
                ImmutableList.of(a.getInfo().getLegalIdentities().get(0).getOwningKey()));
        builder.addOutputState(output, IOUContract.IOU_CONTRACT_ID);
        SignedTransaction ptx = a.getServices().signInitialTransaction(builder);

        FilteredTransaction ftx = new ExchangeRateOracleFlow(ptx)
                .createFilteredTransaction(c.getInfo().getLegalIdentities().get(0), ptx);

        assert(ftx != null);
        assertEquals(1, ftx.getCommands().size());
        assertEquals(1.25, ((IOUContract.Commands.Exchange)
                ftx.getCommands().get(0).getValue()).getRate());
    }
}
