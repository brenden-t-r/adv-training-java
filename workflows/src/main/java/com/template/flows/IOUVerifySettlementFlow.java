package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
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
            // Get notary identity
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Get oracle identity
            Party settlerOracle = Objects.requireNonNull(getServiceHub().getNetworkMapCache().getNodeByLegalName(
                    new CordaX500Name("SettlerOracleService", "New York", "US")
            )).getLegalIdentities().get(0);

            IOUState state = stateToSettle.getState().getData();

            TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addInputState(stateToSettle);
            builder.addOutputState(state.withSettled(), IOUContract.IOU_CONTRACT_ID);
            List<Party> requiredSigners = ImmutableList.of(
                    state.getLender(), state.getBorrower(), settlerOracle);
            builder.addCommand(
                    new IOUContract.Commands.Settle(
                        transactionId, state.amount.toDecimal().doubleValue(),
                        state.amount.getToken().getTokenIdentifier(), settlementAccount),
                    requiredSigners.stream().map(Party::getOwningKey).collect(Collectors.toList())
            );
            builder.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Get Settlement oracle signature and add to SignedTransaction
            TransactionSignature oracleSignature = subFlow(
                    new SettlerOracle.SignOffLedgerPayment(settlerOracle, ptx));

            return ptx.withAdditionalSignature(oracleSignature);
        }

    }

    @InitiatedBy(IOUVerifySettlementFlow.Initiator.class)
    public static class ResponderFlow extends FlowLogic<SignedTransaction> {
        private final FlowSession flowSession;

        public ResponderFlow(FlowSession flowSession){
            this.flowSession = flowSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow{

                private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
                    super(flowSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(req -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker()));
        }
    }
}
