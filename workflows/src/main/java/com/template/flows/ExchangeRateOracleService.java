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

        ExecutorService executor = Executors.newFixedThreadPool(5);

    }

    public Double query(String currencyCode) {
        // Query external data source and return result
        // In practice this would be an external call to a real service
        if (currencyCode.equals("USD")) {
            return 1.5;
        } else if (currencyCode.equals("GBP")) {
            return 1.8;
        } else throw new IllegalArgumentException("Unsupported currency.");
    }

    public TransactionSignature sign(FilteredTransaction ftx) throws FilteredTransactionVerificationException {
        ftx.verify(); // Check the partial Merkle tree is valid.

        if (ftx.checkWithFun(this::isCommandCorrect)){
            return serviceHub.createSignature(ftx, myKey);
        } else throw new IllegalArgumentException("Invalid transaction.");
    }

    private Boolean isCommandCorrect(Object elem) {
        if (elem instanceof Command) {
            Command command = ((Command) elem);
            if (command.getValue().getClass().equals(IOUContract.Commands.Exchange.class)) {
                IOUContract.Commands.Exchange exchange =
                        (IOUContract.Commands.Exchange)command.getValue();
                return command.getSigners().contains(myKey) &&
                        query(exchange.getCurrency()).equals(exchange.getRate());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}
