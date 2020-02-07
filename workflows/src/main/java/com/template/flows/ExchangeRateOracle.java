package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;

public class ExchangeRateOracle {

    @InitiatingFlow
    public static class QueryExchangeRate extends FlowLogic<Double> {

        private Party oracle;
        private String currencyCode;

        public QueryExchangeRate(Party oracle, String currencyCode) {
            this.oracle = oracle;
            this.currencyCode = currencyCode;
        }

        @Suspendable
        @Override
        public Double call() throws FlowException {
            return initiateFlow(oracle).sendAndReceive(Double.class, currencyCode)
                    .unwrap(it -> it);
        }
    }

    @InitiatedBy(QueryExchangeRate.class)
    public static class QueryHandler extends FlowLogic<Void> {

        FlowSession session;

        public QueryHandler(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            String request = session
                    .receive(String.class)
                    .unwrap(it -> it);
            Double response = getServiceHub()
                    .cordaService(ExchangeRateOracleService.class)
                    .query(request);
            session.send(response);
            return null;
        }
    }

    @InitiatingFlow
    public static class SignExchangeRate extends FlowLogic<TransactionSignature> {

        private Party oracle;
        private FilteredTransaction ftx;

        public SignExchangeRate(Party oracle, FilteredTransaction filteredTransaction) {
            this.oracle = oracle;
            this.ftx = filteredTransaction;
        }

        @Suspendable
        @Override
        public TransactionSignature call() throws FlowException {
            FlowSession session = initiateFlow(oracle);
            return session
                    .sendAndReceive(TransactionSignature.class, ftx)
                    .unwrap(it -> it);
        }
    }

    @InitiatedBy(SignExchangeRate.class)
    public static class SignHandler extends FlowLogic<Void> {

        FlowSession session;

        public SignHandler(FlowSession session) {
            this.session = session;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            try {
                FilteredTransaction request = session
                        .receive(FilteredTransaction.class)
                        .unwrap(it -> it);
                TransactionSignature response = getServiceHub()
                        .cordaService(ExchangeRateOracleService.class)
                        .sign(request);
                session.send(response);

            } catch (FilteredTransactionVerificationException e) {
                throw new FlowException(e.getMessage());
            }
            return null;
        }
    }

}
