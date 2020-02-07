package com.template;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.template.states.TokenSdkExamples;
import net.corda.core.identity.Party;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        NonFungibleToken result = TokenSdkExamples.createNonFungibleFixedToken(issuer, holder);
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
        NonFungibleToken result = TokenSdkExamples.createNonFungibleEvolvableToken(issuer, holder);
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
        FungibleToken result = TokenSdkExamples.createFungibleFixedToken(issuer, holder, 1000L);
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
        FungibleToken result = TokenSdkExamples.createFungibleEvolvableToken(issuer, holder, 1000L);
        assert(FungibleToken.class.isAssignableFrom(result.getClass()));
        assertEquals(TokenSdkExamples.ExampleEvolvableToken.class, result.getTokenType().getTokenClass());
        assertEquals(1000, result.getAmount().getQuantity());
    }


}
