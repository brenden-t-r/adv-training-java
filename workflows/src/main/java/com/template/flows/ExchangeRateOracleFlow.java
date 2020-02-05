package com.template.flows;

import net.corda.core.contracts.Command;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class ExchangeRateOracleFlow extends FlowLogic<SignedTransaction> {

    SignedTransaction stx;

    public ExchangeRateOracleFlow(SignedTransaction stx) {
        this.stx = stx;
    }

    public FilteredTransaction createFilteredTransaction(Party oracle, SignedTransaction stx) {
        return stx.buildFilteredTransaction(it -> {
            if (it instanceof Command) {
                return (((Command) it).getSigners()
                        .contains(oracle.getOwningKey()));
            } else {
                return false;
            }
        });
    }

    @Override
    public SignedTransaction call() throws FlowException {
        return null;
    }
}
