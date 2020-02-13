package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.states.IOUState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;
import java.util.stream.Collectors;

public class IOUSettleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator  extends FlowLogic<SignedTransaction> {

        StateAndRef stateToSettle;
        String settlementAccount;
        String settlementCurrency = "USD";

        public Initiator(StateAndRef stateToSettle, String settlementAccount) {
            this.stateToSettle = stateToSettle;
            this.settlementAccount = settlementAccount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            /*
             Novate IOU
             */
            SignedTransaction oracleSignedTx = subFlow(new IOUNovateFlow.Initiator(
                    stateToSettle, settlementCurrency, settlementAccount
            ));
            List<FlowSession> sessions = createCounterpartySessions(oracleSignedTx);
            oracleSignedTx = subFlow(new CollectSignaturesFlow(oracleSignedTx, sessions));
            oracleSignedTx = subFlow(new FinalityFlow(oracleSignedTx, sessions));
            IOUState novatedIOU = (IOUState)oracleSignedTx.getTx().getOutputStates().get(0);

            /*
             Make offledger payment
             */
            String transactionId = subFlow(new OffLedgerPaymentFlow(
                    settlementAccount, novatedIOU.getAmount().toDecimal().doubleValue()
            ));

            /*
            Settle
             */
            StateAndRef<IOUState> novatedStateRef = vaultQuery(novatedIOU.getLinearId());
            SignedTransaction settlerSignedTx = subFlow(new IOUVerifySettlementFlow.Initiator(
                    novatedStateRef, settlementAccount, transactionId
            ));

            // Collect counter-party signature and finalize
            sessions = createCounterpartySessions(settlerSignedTx);
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(settlerSignedTx, sessions));
            return subFlow(new FinalityFlow(stx, sessions));
        }

        private StateAndRef<IOUState> vaultQuery(UniqueIdentifier linearId) {
            return getServiceHub().getVaultService().queryBy(IOUState.class,
                    new QueryCriteria.LinearStateQueryCriteria(
                            null, ImmutableList.of(linearId), Vault.StateStatus.UNCONSUMED, null))
                    .getStates().get(0);
        }

        private List<FlowSession> createCounterpartySessions(SignedTransaction stx) {
            List<Party> otherParties = stx.getTx().getOutputStates().get(0)
                    .getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            List<FlowSession> sessions = otherParties.stream().map(this::initiateFlow).collect(Collectors.toList());
            return sessions;
        }
    }

    @InitiatedBy(IOUSettleFlow.Initiator.class)
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


