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

@InitiatingFlow
public class IOUTokenIssueFlow extends FlowLogic<SignedTransaction> {

    private int tokenAmount;

    public IOUTokenIssueFlow(int tokenAmount) {
        this.tokenAmount = tokenAmount;
    }

    @Override
    public SignedTransaction call() throws FlowException {
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(),
                new IOUToken("CUSTOM_TOKEN", 0));
        FungibleToken fungibleTokens = new FungibleToken(
                new Amount<>(tokenAmount, issuedTokenType), getOurIdentity(),
                getServiceHub().getCordappProvider().getContractAttachmentID(IOUContract.IOU_CONTRACT_ID)
        );
        return subFlow(new IssueTokens(ImmutableList.of(fungibleTokens)));

//        ALTERNATIVE SYNTAX
//        Amount<TokenType> amount = AmountUtilitiesKt.amount(tokenAmount, iouToken);
//        Amount<IssuedTokenType> issuedToken = AmountUtilitiesKt.issuedBy(amount, getOurIdentity());
//        FungibleToken fungibleToken = TokenUtilitiesKt.heldBy(issuedToken, getOurIdentity());
    }
}

