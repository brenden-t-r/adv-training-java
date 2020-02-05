package com.template.states;

import com.google.common.collect.ImmutableList;
import com.template.contracts.IOUContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BelongsToContract(IOUContract.class)
public class IOUTokenState implements LinearState {

    Amount<IOUToken> amount;
    Party lender;
    Party borrower;
    Amount<IOUToken> paid;
    UniqueIdentifier linearId;

    public IOUTokenState(Amount<IOUToken> amount, Party lender, Party borrower) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = new Amount(0, amount.getToken());
        this.linearId = new UniqueIdentifier();
    }

    @ConstructorForDeserialization
    public IOUTokenState(Amount<IOUToken> amount, Party lender, Party borrower, Amount<IOUToken> paid, UniqueIdentifier linearId) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    public Amount<IOUToken> getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public Amount<IOUToken> getPaid() {
        return paid;
    }
}
