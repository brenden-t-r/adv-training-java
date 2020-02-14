package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.template.contracts.IOUContract;
import com.template.flows.DeliveryVersusPaymentTokenFlow;
import com.template.flows.IOUTokenIssueFlow;
import com.template.states.IOUState;
import com.template.states.TokenSdkExamples;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Signed;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.template.states.TokenSdkExamples.createFungibleFixedToken;
import static com.template.states.TokenSdkExamples.createNonFungibleFixedToken;
import static groovy.util.GroovyTestCase.assertEquals;

public class TokenSdkExercises {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode();

    public TokenSdkExercises() { }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    /**
     * TODO: Implement [ExampleFixedToken].
     * Hint:
     * - Fixed Tokens need to extend the [TokenType] class from Token SDK.
     * - [TokenType] classes need to override two fields:
     * -- [tokenIdentifier], String identifying the token, used in contracts to token states are of the same type.
     * -- [fractionDigits], Int defining the number of decimal digits (ex. 2 => 10.00)
     */
    @Test
    public void testCreateFixedToken() {
        assert(TokenType.class.isAssignableFrom(TokenSdkExamples.ExampleFixedToken.class));
    }

    /**
     * TODO: Implement [ExampleEvolvableToken].
     * Hint:
     * - Evolvable Tokens need to extend the [EvolvableTokenType] class from Token SDK.
     * - [EvolvableTokenType] classes need to override three fields:
     * -- [maintainers], List<Party> which specifies which parties will be notified when the token is updated.
     * -- [fractionDigits], Int defining the number of decimal digits (ex. 2 => 10.00).
     * -- [linearId] - remember that Evolvable Tokens have a linearId since they can evolve over time.
     * - In addition to these fields, we can have any number of additional fields like any other [LinearState].
     */
    @Test
    public void testCreateEvolvableToken() {
        assert(EvolvableTokenType.class.isAssignableFrom(TokenSdkExamples.ExampleEvolvableToken.class));
    }

    /**
     * TODO: Implement [createNonFungibleFixedToken] method.
     * Hint:
     * - Tokens can be Fixed versus Evolvable, but also Fungible versus Non-fungible.
     * - For our method, we want to return a [NonFungibleToken] object containing an [ExampleFixedToken]
     * - [NonFungibleToken] takes 3 parameters:
     * -- an [IssuedTokenType] instance, a token holder [Party], and a linearId to identify this [NonFungibleToken].
     * -- [IssuedTokenType] is a wrapper object that pairs a [TokenType] with a issuer [Party].
     */
    @Test
    public void testCreateNonFungibleFixedToken() {
        Party issuer = b.getInfo().getLegalIdentities().get(0);
        Party holder = a.getInfo().getLegalIdentities().get(0);
        NonFungibleToken result = createNonFungibleFixedToken(issuer, holder,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));
        assert(NonFungibleToken.class.isAssignableFrom(result.getClass()));
        assertEquals(TokenSdkExamples.ExampleFixedToken.class, result.getTokenType().getTokenClass());
    }

    /**
     * TODO: Implement [createNonFungibleEvolvableToken] method.
     * Hint:
     * - Now we want to create a [NonFungibleToken] containing an [ExampleEvolvableToken]
     * - This will be the same as in the previous exercise, except that since EvolvableTokens use
     * a [TokenPointer] to pair the evolvable token data [LinearState] with the actual [TokenType].
     * - Our [IssuedTokenType] will wrap our [TokenPointer] with the issuer [Party].
     * - The [TokenPointer] is a [TokenType] implementation that wraps a [LinearPointer] with a
     * displayTokenSize which will be the [fractionDigits] from our [ExampleEvolvableToken].
     * - The [LinearPointer] takes as parameter the linear Id of our [ExampleEvolvableToken]
     * and the class definition for the token type.
     * -- Hint: You can use the [ExampleEvolvableToken::class.java] notation.
     */
    @Test
    public void testCreateNonFungibleEvolvableToken() {
        Party issuer = b.getInfo().getLegalIdentities().get(0);
        Party holder = a.getInfo().getLegalIdentities().get(0);
        NonFungibleToken result = TokenSdkExamples.createNonFungibleEvolvableToken(issuer, holder,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));
        assert(NonFungibleToken.class.isAssignableFrom(result.getClass()));
        assertEquals(TokenSdkExamples.ExampleEvolvableToken.class, result.getTokenType().getTokenClass());
    }

    /**
     * TODO: Implement [createFungibleFixedToken] method.
     * Hint:
     * - Now we want to create a [FungibleToken] containing an [ExampleFixedToken]
     * - When creating a [FungibleToken] we need to supply an [Amount] of the [IssuedTokenType]
     * as well as the [Party] which is the owner of these tokens.
     */
    @Test
    public void testCreateFungibleFixedToken() {
        Party issuer = b.getInfo().getLegalIdentities().get(0);
        Party holder = a.getInfo().getLegalIdentities().get(0);
        FungibleToken result = TokenSdkExamples.createFungibleFixedToken(issuer, holder, 1000L,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));
        assert(FungibleToken.class.isAssignableFrom(result.getClass()));
        assertEquals(TokenSdkExamples.ExampleFixedToken.class, result.getTokenType().getTokenClass());
        assertEquals(1000, result.getAmount().getQuantity());
    }

    /**
     * TODO: Implement [createFungibleEvolvableToken] method.
     * Hint:
     * - Now we want to create a [FungibleToken] containing an [ExampleEvolvableToken]
     * - Use the [createFungibleFixedToken] and [createNonFungibleEvolvableToken] methods as a guide here.
     */
    @Test
    public void testCreateFungibleEvolvableToken() {
        Party issuer = b.getInfo().getLegalIdentities().get(0);
        Party holder = a.getInfo().getLegalIdentities().get(0);
        FungibleToken result = TokenSdkExamples.createFungibleEvolvableToken(issuer, holder, 1000L,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));
        assert(FungibleToken.class.isAssignableFrom(result.getClass()));
        assertEquals(TokenSdkExamples.ExampleEvolvableToken.class, result.getTokenType().getTokenClass());
        assertEquals(1000, result.getAmount().getQuantity());
    }

    /**
     * TODO: Convert IOUState to be in terms of an Amount of Token SDK Tokens rather than Currency
     * Hint:
     *  -
     */
    @Test
    public void hasIOUAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the amount field exist?
        Field field = IOUState.class.getDeclaredField("amount");

        // Is the amount field of the correct type?
        assertEquals(field.getType(), Amount.class);

        // Does the amount field have the correct paramerized type?
        Type signature = ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
        assertEquals(signature, TokenType.class);
    }

    /**
     * TODO: Implement [IOUTokenIssueFlow].
     * Hint:
     * - Now we know how to create and instantiate Fixed/Evolvable Fungible/Non-fungible Tokens,
     * the next step is to actually ISSUE them on the ledger as immutable facts.
     * - To do this we will implement the [IOUTokenIssueFlow] which will be a simple flow
     * that will create and issue specified amount of FungibleTokens using our fixed IOUToken.
     * - Once we have created the [FungibleToken] object, we subFlow the [IssueTokens] Flow from
     * the Token SDK.
     * - The [IssueTokens] Flow simply takes as argument a list containing the [FungibleToken]s to
     * issue. In our case this will be a list containing our single [FungibleToken] instance.
     * -- You can use the [listOf] Kotlin command to easily make a list.
     */
    @Test
    public void implementIOUTokenIssueFlow() throws ExecutionException, InterruptedException {
        Future<SignedTransaction> future = b.startFlow(new IOUTokenIssueFlow(25));
        network.runNetwork();
        SignedTransaction stx = future.get();

        assertEquals(stx.getTx().getOutputStates().size(), 1);
        assertEquals(((FungibleToken) stx.getTx().getOutputStates().get(0)).getAmount().getQuantity(), 25);
    }

    /**
     * TODO: Implement [DeliveryVersusPaymentTokenFlow].
     * Hint:
     * - This flow will implement a simple delivery versus payment use case to exchange two different
     * token types between two parties in an atomic transaction.
     * - First, we'll need to create a [TransactionBuilder] with a [Notary] identity.
     * - Then, we need to create a [FungibleToken] of some amount of the [ExampleFixedToken]
     * in the [ourPayment] parameter.
     * - Then, we need to add a Move command to our TransactionBuilder for both the payment and
     * counter party asset.
     * -- Here, the [addMoveFungibleTokens] and [addMoveNonFungibleTokens] helper methods from
     * the Token SDK will come in handy.
     * -- Control+Click the above helper methods to see the usage.
     * - Finally, to get our unit test passing, use the [serviceHub] to sign the initial transaction
     * and return the partially signed transaction.
     */
    @Test
    public void implementDeliveryVersusPaymentFlow() throws ExecutionException, InterruptedException, TransactionResolutionException, AttachmentResolutionException {
        Party partyA = a.getInfo().getLegalIdentities().get(0);
        Party partyB = b.getInfo().getLegalIdentities().get(0);
        FungibleToken fungibleToken = createFungibleFixedToken(partyA, partyA, 1000L,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));
        NonFungibleToken nonFungibleToken = createNonFungibleFixedToken(partyB, partyB,
                a.getServices().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID));

        Future<SignedTransaction> fungibleFuture = a.startFlow(new IssueTokensFlow(fungibleToken));
        network.runNetwork();
        SignedTransaction stx = fungibleFuture.get();
        assertEquals(1, stx.getTx().getOutputStates().size());

        Future<SignedTransaction> nonFungibleFuture = b.startFlow(new IssueTokens(
                ImmutableList.of(nonFungibleToken), ImmutableList.of(partyA)));
        network.runNetwork();
        SignedTransaction stx2 = nonFungibleFuture.get();
        assertEquals(1, stx2.getTx().getOutputStates().size());

        List<StateAndRef<ContractState>> states = a.getServices().getVaultService()
                .queryBy(ContractState.class).getStates();

        Future<SignedTransaction> dvpFuture = a.startFlow(new DeliveryVersusPaymentTokenFlow(
                new TokenSdkExamples.ExampleFixedToken("CUSTOMTOKEN", 2),
                new TokenSdkExamples.ExampleFixedToken("CUSTOMTOKEN", 2),
                partyB
        ));
        network.runNetwork();
        SignedTransaction stx3 = dvpFuture.get();

        assertEquals(2, stx3.getTx().getOutputStates().size());
        assertEquals(1, stx3.getTx().toLedgerTransaction(a.getServices()).outputsOfType(FungibleToken.class).size());
        assertEquals(1, stx3.getTx().toLedgerTransaction(a.getServices()).outputsOfType(NonFungibleToken.class).size());
        FungibleToken f = stx3.getTx().toLedgerTransaction(a.getServices()).outputsOfType(FungibleToken.class).get(0);
        NonFungibleToken nf = stx3.getTx().toLedgerTransaction(a.getServices()).outputsOfType(NonFungibleToken.class).get(0);
        assertEquals(partyB, f.getHolder());
        assertEquals(partyA, nf.getHolder());
    }

}
