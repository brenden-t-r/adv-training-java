package com.template.flows;

import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;

import java.security.PublicKey;
import java.security.SignedObject;

@CordaService
public class SettlerOracleService extends SingletonSerializeAsToken {

    private ServiceHub serviceHub;
    private PublicKey myKey;

    public SettlerOracleService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.myKey = serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey();
    }

    public boolean query(String transactionId, Double amount, String recipientAccountNumber) {
        return serviceHub.cordaService(OffLedgerPaymentRailService.class).verifyTransaction(
                transactionId, amount, recipientAccountNumber);
    }

    public TransactionSignature sign(String transactionId, SignedTransaction stx) throws FilteredTransactionVerificationException {
        IOUState iouState = (IOUState) stx.getTx().getOutputs().get(0).getData();
        if (query(transactionId, iouState.getNovatedAmount(), iouState.getSettlementAccount())) {
            return serviceHub.createSignature(stx, myKey);
        } else {
            throw new IllegalArgumentException("Invalid transaction.");
        }
    }

}
