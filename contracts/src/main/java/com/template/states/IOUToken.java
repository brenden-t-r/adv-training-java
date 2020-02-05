package com.template.states;

import com.r3.corda.lib.tokens.contracts.types.TokenType;

public class IOUToken extends TokenType {

    private String tokenIdentifier;
    private Integer fractionDigits;

    public IOUToken(String tokenIdentifier, Integer fractionDigits) {
        super(tokenIdentifier, fractionDigits);
    }

}
