package com.template.flows;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilitiesKt;
import com.template.states.IOUToken;
import com.template.states.TokenSdkExamples;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.TokenUtilitiesKt;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilitiesKt;

import java.util.List;

@InitiatingFlow
public class DeliveryVersusPaymentTokenFlow extends FlowLogic<SignedTransaction> {
    private TokenSdkExamples.ExampleFixedToken ourpayment;
    private TokenSdkExamples.ExampleFixedToken counterPartyAsset;
    private Party counterParty;

    public DeliveryVersusPaymentTokenFlow(TokenSdkExamples.ExampleFixedToken ourpayment, TokenSdkExamples.ExampleFixedToken counterPartyAsset, Party counterParty) {
        this.ourpayment = ourpayment;
        this.counterPartyAsset = counterPartyAsset;
        this.counterParty = counterParty;
    }

    @Override
    public SignedTransaction call() {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        IssuedTokenType issuedTokenType = new IssuedTokenType(getOurIdentity(), ourpayment);
        Amount<TokenType> ourPaymentAmount = new Amount<>(1000, issuedTokenType);

        QueryCriteria queryCriteria = QueryUtilitiesKt.tokenAmountWithIssuerCriteria(ourpayment, getOurIdentity());
        TransactionBuilder builder = new TransactionBuilder(notary);
        builder = MoveTokensUtilitiesKt.addMoveFungibleTokens(
                builder, getServiceHub(), ourPaymentAmount, counterParty, getOurIdentity(), queryCriteria);
        builder = MoveTokensUtilitiesKt.addMoveNonFungibleTokens(
                builder, getServiceHub(), counterPartyAsset, getOurIdentity()
        );

        return getServiceHub().signInitialTransaction(builder);
    }
}
