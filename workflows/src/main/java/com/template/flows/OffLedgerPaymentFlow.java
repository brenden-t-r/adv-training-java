package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class OffLedgerPaymentFlow extends FlowLogic<String> {

    String recipientAccountNumber;
    Double paymentAmount;

    public OffLedgerPaymentFlow(String recipientAccountNumber, Double paymentAmount) {
        this.recipientAccountNumber = recipientAccountNumber;
        this.paymentAmount = paymentAmount;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {
        return "";
    }
}

