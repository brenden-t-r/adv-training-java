package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.crypto.TransactionSignature;
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

public class IOUVerifySettlementFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator  extends FlowLogic<SignedTransaction> {

        StateAndRef<IOUState> stateToSettle;
        String settlementAccount;
        String transactionId;

        public Initiator(StateAndRef stateToSettle, String settlementAccount, String transcationId) {
            this.stateToSettle = stateToSettle;
            this.settlementAccount = settlementAccount;
            this.transactionId = transcationId;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Get oracle identity
            CordaX500Name oracleName = new CordaX500Name(
                    "SettlerOracleService", "New York","US");

            // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
            return getServiceHub().signInitialTransaction(
                    new TransactionBuilder(null)
            );
        }

    }

    @InitiatedBy(IOUVerifySettlementFlow.Initiator.class)
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
