package com.template.flows;


import com.template.contracts.IOUContract;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;

import java.security.PublicKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CordaService
public class ExchangeRateOracleService extends SingletonSerializeAsToken {

    private ServiceHub serviceHub;
    private PublicKey myKey;

    public ExchangeRateOracleService(ServiceHub serviceHub) {
        this.serviceHub = serviceHub;
        this.myKey = serviceHub.getMyInfo().getLegalIdentities().get(0).getOwningKey();
    }

    public Double query(String currencyCode) {
        return null;
    }

    public TransactionSignature sign(FilteredTransaction ftx) throws FilteredTransactionVerificationException {
        return  null;
    }

}
