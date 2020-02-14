package com.template.states;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokenSdkExamples {

    public static class ExampleFixedToken {}

    public static class ExampleEvolvableToken {}

    public static NonFungibleToken createNonFungibleFixedToken(Party issuer, Party tokenHolder, SecureHash jarHash) {
        return null;
    }

    public static NonFungibleToken createNonFungibleEvolvableToken(Party issuer, Party tokenHolder, SecureHash jarHash) {
        return null;
    }

    public static FungibleToken createFungibleFixedToken(Party issuer, Party tokenHolder, Long tokenQuantity, SecureHash jarHash) {
        return null;
    }

    public static FungibleToken createFungibleEvolvableToken(Party issuer, Party tokenHolder, Long tokenQuantity, SecureHash jarHash) {
        return null;
    }

}
