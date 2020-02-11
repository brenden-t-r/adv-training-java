package com.template.flows;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;

import java.security.PublicKey;


@CordaService
public class OffLedgerPaymentRailService extends SingletonSerializeAsToken {

    private ServiceHub serviceHub;
    private PublicKey myKey;

    public OffLedgerPaymentRailService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.myKey = serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey();
    }

    public boolean verifyTransaction(String transactionId, Double paymentAmount, String recipientAccountNumber) {
        return true;
    }

    public String makePayment(String recipientAccountNumber, Double amount) {
        return "TEST_TRANSACTION_ID_1234";
    }
}
