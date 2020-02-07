package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.template.contracts.IOUContract;
import com.template.flows.ExchangeRateOracle;
import com.template.flows.ExchangeRateOracleFlow;
import com.template.flows.ExchangeRateOracleService;
import com.template.states.IOUState;
import com.template.states.IOUToken;
import com.template.states.IOUTokenState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static groovy.util.GroovyTestCase.assertEquals;
import static org.junit.Assert.fail;

public class OracleExercises {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode(
            new CordaX500Name("ExchangeRateOracleService", "New York", "US")
    );

    public OracleExercises() {
        a.registerInitiatedFlow(ExchangeRateOracle.QueryHandler.class);
        b.registerInitiatedFlow(ExchangeRateOracle.QueryHandler.class);
        a.registerInitiatedFlow(ExchangeRateOracle.SignHandler.class);
        b.registerInitiatedFlow(ExchangeRateOracle.SignHandler.class);
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
     * TODO: Implement a [query] method for the [ExchangeRateOracleService].
     * Hint:
     * - The query method must take a [String] currency code argument and return a [Double] representing
     * the exchange rate received from the off-ledger data source.
     * - For this simple example you can just return a hard-coded value, but in practice you
     * would likely query an external API or file attachment to get the current value.
     */
    @Test
    public void oracleServiceQuery() {
        ExchangeRateOracleService oracle =
                new ExchangeRateOracleService(a.getServices());
        try {
            ExchangeRateOracleService.class.getMethod("query", String.class);
        } catch (NoSuchMethodException e) {
            fail();
        }
        assertEquals(oracle.query("USD").getClass(), Double.class);
    }

    /**
     * TODO: Implement a [sign] method for the [ExchangeRateOracleService].
     * Hint:
     * - The sign method must take a [FilteredTransaction] argument and return the oracle's signature
     * as a [TransactionSignature].
     * - The [FilteredTransaction] will be a Merkle Tree tear-off containing only the command and data
     * needed for the Oracle to verify the exchange rate is correct.
     * - The sign method must verify that the rate provided in the [Exchange] Command is valid.
     * -- In order to do this, we need to see that the command provided in the [FilteredTransaction]
     * is of type [IOUContract.Commands.Exchange] and that the [rate] field provided on the
     * Exchange command is the correct rate given the [currency]. To do this we will need to again
     * call to the [query] function.
     * -- One simple way to perform this check would be to use the [checkWithFun] method of the
     * [FilterTransaction], passing in a reference to a function that will check the [Exchange] command.
     * -- An example use of [checkWithFun] would be ftx.checkWithFun(::isCommandCorrect) where
     * isCommandCorrect refers to a function that takes the [Command] as argument.
     * -- Hint: for the [Command], remember that the actual Command type and data is found within the [value]
     * property.
     */
    @Test
    public void oracleServiceSign() throws FilteredTransactionVerificationException, SignatureException, InvalidKeyException {
        ExchangeRateOracleService oracle =
                new ExchangeRateOracleService(a.getServices());
        try {
            ExchangeRateOracleService.class.getMethod("sign", FilteredTransaction.class);
        } catch (NoSuchMethodException e) {
            fail();
        }

        Party notary = a.getServices().getNetworkMapCache().getNotaryIdentities().get(0);
        TransactionBuilder builder = new TransactionBuilder(notary);
        Double rate = oracle.query("USD");
        IOUTokenState output = new IOUTokenState(
                new Amount(3, new IOUToken("CUSTOM_TOKEN", 2)),
                a.getServices().getMyInfo().getLegalIdentities().get(0),
                b.getServices().getMyInfo().getLegalIdentities().get(0));
        builder.addCommand(
                new IOUContract.Commands.Exchange("USD", rate /* Invalid rate */),
                ImmutableList.of(
                        c.getInfo().getLegalIdentities().get(0).getOwningKey(),
                        a.getInfo().getLegalIdentities().get(0).getOwningKey()));
        builder.addOutputState(output, IOUContract.IOU_CONTRACT_ID);

        SignedTransaction ptx = a.getServices().signInitialTransaction(builder);

        FilteredTransaction ftx = ptx.buildFilteredTransaction(it -> {
            if (it instanceof Command) {
                return (((Command) it).getSigners()
                        .contains(c.getInfo().getLegalIdentities().get(0).getOwningKey())) &&
                        ((Command) it).getValue() instanceof  IOUContract.Commands.Exchange;
            } else {
                return false;
            }
        });

        TransactionSignature oracleSig = oracle.sign(ftx);
        assert(oracleSig.isValid(ftx.getId()));
    }

    /**
     * TODO: Implement the [ExchangeRateOracleFlow].
     * Hint:
     * - In this flow, we will take a partially signed transaction as argument, and utilize
     * our [ExchangeRateOracleService] to provide the exchange rate. This will be allow us to
     * conveniently subFlow this [ExchangeRateOracleFlow] within other flows that require it.
     * - First, we need to get the Oracle [Party] identity using the [serviceHub]'s networkMapCache.
     * - Then, we need to create a [FilteredTransaction] in order to preserve confidentiality
     * and only provide the Oracle with visibility to the exchange rate portion of the transaction.
     * This is handy and still enables us to gather and verify the Oracle's signature.
     * -- Utilize the [createFilteredTransaction] helper function.
     * - Finally we need to require a signature from the Oracle.
     * -- To do this, simply subFlow the [SignExchangeRate] flow.
     * -- Once we have the returned signature, just add it to the transaction with the
     * [withAdditionalSignature] method and return the [SignedTransaction].
     */
    @Test
    public void oracleFlow() throws ExecutionException, InterruptedException, SignatureException {
        Party notary = a.getServices().getNetworkMapCache().getNotaryIdentities().get(0);
        ExchangeRateOracleService oracle =
                new ExchangeRateOracleService(a.getServices());
        TransactionBuilder builder = new TransactionBuilder(notary);

        Future<Double> future = a.startFlow(new ExchangeRateOracle.QueryExchangeRate(
                c.getInfo().getLegalIdentities().get(0), "USD"));
        network.runNetwork();
        Double resultFromOracle = future.get();

        // Update builder with value
        builder.addCommand(
                new IOUContract.Commands.Exchange("USD", resultFromOracle),
                ImmutableList.of(
                        c.getInfo().getLegalIdentities().get(0).getOwningKey(),
                        a.getInfo().getLegalIdentities().get(0).getOwningKey())
        );

        IOUTokenState iouTokenState = new IOUTokenState(
                new Amount(5, new IOUToken("CUSTOM_TOKEN", 2)),
                a.getInfo().getLegalIdentities().get(0),
                b.getInfo().getLegalIdentities().get(0));
        FiatCurrency.Companion.getInstance("USD");
        builder.addOutputState(new IOUState(
                new Amount(iouTokenState.getAmount().getQuantity(), FiatCurrency.Companion.getInstance("USD")),
                iouTokenState.getLender(),
                iouTokenState.getBorrower()), IOUContract.IOU_CONTRACT_ID);
        SignedTransaction ptx = a.getServices().signInitialTransaction(builder);

        Future<SignedTransaction> oracleFuture = a.startFlow(new ExchangeRateOracleFlow(ptx));
        network.runNetwork();
        SignedTransaction signedTx = oracleFuture.get();

        // Check that oracle signature is present
        signedTx.verifyRequiredSignatures();
    }

}
