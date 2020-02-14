package com.template.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TokenUtilitiesKt;
import com.r3.corda.lib.tokens.money.UtilitiesKt;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.*;
import com.template.contracts.IOUContract;
import com.template.states.IOUToken;
import net.corda.core.contracts.Amount;
import net.corda.core.cordapp.CordappProvider;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

@InitiatingFlow
public class IOUTokenIssueFlow extends FlowLogic<SignedTransaction> {

    private int tokenAmount;

    public IOUTokenIssueFlow(int tokenAmount) {
        this.tokenAmount = tokenAmount;
    }

    @Override
    public SignedTransaction call() throws FlowException {
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return getServiceHub().signInitialTransaction(
                new TransactionBuilder(null)
        );
    }
}

