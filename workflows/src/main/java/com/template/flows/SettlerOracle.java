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
        private String transactionId;

        public SignOffLedgerPayment(Party oracle, SignedTransaction stx, String transactionId) {
            this.oracle = oracle;
            this.stx = stx;
            this.transactionId = transactionId;
        }

        @Suspendable
        @Override
        public TransactionSignature call() throws FlowException {
            FlowSession session = initiateFlow(oracle);
            return session
                    .sendAndReceive(TransactionSignature.class, new Pair<>(transactionId, stx))
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
                Pair<String, SignedTransaction> request = (Pair<String, SignedTransaction>)session
                        .receive(Pair.class)
                        .unwrap(it -> it);
                TransactionSignature response = getServiceHub()
                        .cordaService(SettlerOracleService.class)
                        .sign(request.getKey(), request.getValue());
                session.send(response);

            } catch (FilteredTransactionVerificationException e) {
                throw new FlowException(e.getMessage());
            }
            return null;
        }
    }
}
