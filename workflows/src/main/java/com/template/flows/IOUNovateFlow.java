package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUNovateFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator  extends FlowLogic<SignedTransaction> {

        StateAndRef stateToSettle;
        String settlementAccount;
        String settlementCurrency;

        Initiator(StateAndRef stateToSettle, String settlementCurrency, String settlementAccount) {
            this.stateToSettle = stateToSettle;
            this.settlementCurrency = settlementCurrency;
            this.settlementAccount = settlementAccount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Get notary identity
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Get oracle identities
            Party fxOracle = Objects.requireNonNull(getServiceHub().getNetworkMapCache().getNodeByLegalName(
                    new CordaX500Name("ExchangeRateOracleService", "New York", "US")
            )).getLegalIdentities().get(0);

            IOUState state = (IOUState)stateToSettle.getState().getData();

            Double fxRate = getServiceHub().cordaService(ExchangeRateOracleService.class).query(settlementCurrency);
            Double novatedAmount = fxRate * state.getAmount().getQuantity();
            TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addInputState(stateToSettle);
            builder.addOutputState(state
                            .withNewAmount(new Amount<>(novatedAmount.longValue(), FiatCurrency.Companion.getInstance(settlementCurrency))),
                    IOUContract.IOU_CONTRACT_ID);
            List<Party> requiredSigners = ImmutableList.of(
                    state.getLender(), state.getBorrower(), fxOracle);
            builder.addCommand(new IOUContract.Commands.Novate(settlementCurrency, fxRate),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList()));
            builder.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Get FxRate oracle signature and add to SignedTransaction
            return subFlow(new ExchangeRateOracleFlow(ptx));
        }
    }

    @InitiatedBy(IOUNovateFlow.Initiator.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {
        private final FlowSession flowSession;
        private SecureHash txWeJustSigned;

        public ResponderFlow(FlowSession flowSession){
            this.flowSession = flowSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            class SignTxFlow extends SignTransactionFlow {

                private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
                    super(flowSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    txWeJustSigned = stx.getId();
                }
            }

            SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());
            subFlow(signTxFlow);
            return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));
        }
    }
}
