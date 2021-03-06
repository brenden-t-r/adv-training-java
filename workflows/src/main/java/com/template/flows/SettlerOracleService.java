package com.template.flows;

import com.template.contracts.IOUContract;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredTransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;

import java.security.PublicKey;

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

    public TransactionSignature sign(SignedTransaction stx) throws FilteredTransactionVerificationException {
        Command command = stx.getTx().getCommands().get(0);

        if (command.getValue().getClass().equals(IOUContract.Commands.Settle.class)) {
            IOUContract.Commands.Settle settle =
                    (IOUContract.Commands.Settle)command.getValue();

            if (query(settle.getTransactionId(), settle.getNovatedAmount(), settle.getSettlementAccount())) {
                return serviceHub.createSignature(stx, myKey);
            } else {
                throw new IllegalArgumentException("Invalid transaction.");
            }
        } else {
            throw new IllegalArgumentException("Invalid transaction.");
        }
    }

}
