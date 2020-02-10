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

    public static class ExampleFixedToken extends TokenType {
        public ExampleFixedToken(String tokenIdentifier, int fractionDigits) {
            super(tokenIdentifier, fractionDigits);
        }
    }

    public static class ExampleEvolvableToken extends EvolvableTokenType {
        List<Party> maintainers;
        int fractionDigits;
        String exampleDataProperty;
        UniqueIdentifier linearId;

        ExampleEvolvableToken(List<Party> maintainers, int fractionDigits, String exampleDataProperty) {
            super();
            this.maintainers = maintainers;
            this.fractionDigits = fractionDigits;
            this.exampleDataProperty = exampleDataProperty;
            this.linearId = new UniqueIdentifier();
        }

        @Override
        public int getFractionDigits() {
            return fractionDigits;
        }

        @NotNull
        @Override
        public List<Party> getMaintainers() {
            return maintainers;
        }

        @NotNull
        @Override
        public UniqueIdentifier getLinearId() {
            return linearId;
        }
    }

    public static NonFungibleToken createNonFungibleFixedToken(Party issuer, Party tokenHolder, SecureHash jarHash) {
        ExampleFixedToken token = new ExampleFixedToken("CUSTOMTOKEN", 0);
        IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, token);
        return new NonFungibleToken(issuedTokenType, tokenHolder, new UniqueIdentifier(), jarHash);
    }

    public static NonFungibleToken createNonFungibleEvolvableToken(Party issuer, Party tokenHolder, SecureHash jarHash) {
        ExampleEvolvableToken token = new
                ExampleEvolvableToken(ImmutableList.of(), 0, "test");
        LinearPointer<ExampleEvolvableToken> linearPointer = new LinearPointer<>(
                token.getLinearId(), ExampleEvolvableToken.class
        );
        TokenPointer tokenPointer = new TokenPointer<>(linearPointer, token.fractionDigits);
        IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, tokenPointer);
        return new NonFungibleToken(issuedTokenType, tokenHolder, new UniqueIdentifier(), jarHash);
    }

    public static FungibleToken createFungibleFixedToken(Party issuer, Party tokenHolder, Long tokenQuantity, SecureHash jarHash) {
        ExampleFixedToken token = new ExampleFixedToken("CUSTOMTOKEN", 0);
        IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, token);
        return new FungibleToken(new Amount<>(tokenQuantity, issuedTokenType), tokenHolder, jarHash);
    }

    public static FungibleToken createFungibleEvolvableToken(Party issuer, Party tokenHolder, Long tokenQuantity, SecureHash jarHash) {
        ExampleEvolvableToken token = new
                ExampleEvolvableToken(ImmutableList.of(), 0, "test");
        LinearPointer<ExampleEvolvableToken> linearPointer = new LinearPointer<>(
                token.getLinearId(), ExampleEvolvableToken.class
        );
        TokenPointer tokenPointer = new TokenPointer<>(linearPointer, token.fractionDigits);
        IssuedTokenType issuedTokenType = new IssuedTokenType(issuer, tokenPointer);
        return new FungibleToken(new Amount<>(tokenQuantity, issuedTokenType), tokenHolder, jarHash);
    }

}
