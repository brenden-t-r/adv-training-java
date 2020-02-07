package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import com.template.flows.ExchangeRateOracle.*;

@InitiatingFlow
@StartableByRPC
public class ExchangeRateOracleFlow extends FlowLogic<SignedTransaction> {

    SignedTransaction ptx;

    public ExchangeRateOracleFlow(SignedTransaction ptx) {
        this.ptx = ptx;
    }

    public FilteredTransaction createFilteredTransaction(Party oracle, SignedTransaction stx) {
        return stx.buildFilteredTransaction(it -> {
            if (it instanceof Command) {
                return (((Command) it).getSigners()
                        .contains(oracle.getOwningKey()));
            } else {
                return false;
            }
        });
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // Get oracle identity
        CordaX500Name oracleName = new CordaX500Name(
                "ExchangeRateOracleService", "New York","US");
        Party oracle = getServiceHub().getNetworkMapCache().getNodeByLegalName(oracleName)
                .getLegalIdentities().get(0);

        // Create MerkleTree "Tear Off" for confidentiality
        FilteredTransaction filteredTx = createFilteredTransaction(oracle, ptx);

        // Request Oracle signature
        TransactionSignature oracleSignature = subFlow(
                new SignExchangeRate(oracle, filteredTx));
        SignedTransaction stx = ptx.withAdditionalSignature(oracleSignature);

        return stx;
    }
}
