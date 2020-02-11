package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import javafx.util.Pair;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;

public class SettlerOracle {
    @InitiatingFlow
    public static class SignOffLedgerPayment extends FlowLogic<TransactionSignature> {

        private Party oracle;
        private SignedTransaction stx;

        public SignOffLedgerPayment(Party oracle, SignedTransaction stx) {
            this.oracle = oracle;
            this.stx = stx;
        }

        @Suspendable
        @Override
        public TransactionSignature call() throws FlowException {
            FlowSession session = initiateFlow(oracle);
            return session
                    .sendAndReceive(TransactionSignature.class, stx)
                    .unwrap(it -> it);
        }
    }

    @InitiatedBy(SettlerOracle.SignOffLedgerPayment.class)
    public static class SignHandler extends FlowLogic<Void> {

        FlowSession session;

        public SignHandler(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            try {
                SignedTransaction request = session
                        .receive(SignedTransaction.class)
                        .unwrap(it -> it);
                TransactionSignature response = getServiceHub()
                        .cordaService(SettlerOracleService.class)
                        .sign(request);
                session.send(response);

            } catch (FilteredTransactionVerificationException e) {
                throw new FlowException(e.getMessage());
            }
            return null;
        }
    }
}
