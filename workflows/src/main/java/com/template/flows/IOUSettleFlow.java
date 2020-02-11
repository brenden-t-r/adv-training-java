package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.CordaService;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUSettleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator  extends FlowLogic<SignedTransaction> {

        StateAndRef<IOUState> stateToSettle;
        String settlementAccount;

        private final String CURRENCY = "USD";

        public Initiator(StateAndRef stateToSettle, String settlementAccount) {
            this.stateToSettle = stateToSettle;
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
            Party settlerOracle = Objects.requireNonNull(getServiceHub().getNetworkMapCache().getNodeByLegalName(
                    new CordaX500Name("SettlerOracleService", "New York", "US")
            )).getLegalIdentities().get(0);

            IOUState state = stateToSettle.getState().getData();

            /*
             Novate IOU and update Settlement terms
             */
            Double fxRate = getServiceHub().cordaService(ExchangeRateOracleService.class).query(CURRENCY);
            Double novatedAmount = fxRate * state.getAmount().getQuantity();
            TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addInputState(stateToSettle);
            builder.addOutputState(state
                    .withNovatedAmount(CURRENCY, novatedAmount)
                    .withSettlementAccount(settlementAccount), IOUContract.IOU_CONTRACT_ID);
            List<Party> requiredSigners = ImmutableList.of(
                    state.getLender(), state.getBorrower(), fxOracle);
            builder.addCommand(new IOUContract.Commands.Novate(CURRENCY, fxRate),
                    requiredSigners.stream().map(it -> it.getOwningKey()).collect(Collectors.toList()));
            builder.verify(getServiceHub());
            SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Get FxRate oracle signature and add to SignedTransaction
            SignedTransaction fxSigned = subFlow(new ExchangeRateOracleFlow(ptx));

            // Collect counter-party signature and finalize
            List<Party> otherParties = state.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(fxSigned, sessions));
            SignedTransaction novatedTx = subFlow(new FinalityFlow(stx));

            /*
             Make offledger payment
             */
            IOUState novatedIOU = (IOUState)novatedTx.getTx().getOutputStates().get(0);
            String transactionId = getServiceHub().cordaService(OffLedgerPaymentRailService.class).makePayment(
                    novatedIOU.getSettlementAccount(), novatedIOU.getNovatedAmount()
            );

            /*
            Settle
             */
            StateAndRef<IOUState> novatedStateRef = vaultQuery(novatedIOU.getLinearId());
            builder = new TransactionBuilder(notary);
            builder.addInputState(novatedStateRef);
            builder.addOutputState(novatedIOU.withSettled(), IOUContract.IOU_CONTRACT_ID);
            requiredSigners = ImmutableList.of(
                    state.getLender(), state.getBorrower(), settlerOracle);
            builder.addCommand(new IOUContract.Commands.Settle(transactionId),
                    requiredSigners.stream().map(it -> it.getOwningKey()).collect(Collectors.toList()));
            builder.verify(getServiceHub());
            ptx = getServiceHub().signInitialTransaction(builder);

            // Get Settlement oracle signature and add to SignedTransaction
            TransactionSignature oracleSignature = subFlow(
                    new SettlerOracle.SignOffLedgerPayment(settlerOracle, ptx));
            SignedTransaction settlerSigned = ptx.withAdditionalSignature(oracleSignature);

            // Collect counter-party signature and finalize
            otherParties = state.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());
            stx = subFlow(new CollectSignaturesFlow(settlerSigned, sessions));
            stx = subFlow(new FinalityFlow(stx));
            return stx;
        }

        private StateAndRef<IOUState> vaultQuery(UniqueIdentifier linearId) {
            return getServiceHub().getVaultService().queryBy(IOUState.class,
                    new QueryCriteria.LinearStateQueryCriteria(
                            null, ImmutableList.of(linearId), Vault.StateStatus.UNCONSUMED, null))
                    .getStates().get(0);
        }
    }

    @InitiatedBy(IOUSettleFlow.Initiator.class)
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


