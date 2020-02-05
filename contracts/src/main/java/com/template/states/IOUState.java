package com.template.states;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.template.contracts.IOUContract;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@BelongsToContract(IOUContract.class)
public class IOUState implements QueryableState, LinearState {

    public final Amount<TokenType> amount;
    public final Party lender;
    public final Party borrower;
    public final Amount<TokenType> paid;
    private final UniqueIdentifier linearId;

    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    private IOUState(Amount<TokenType> amount, Party lender, Party borrower, Amount<TokenType> paid, UniqueIdentifier linearId){
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
    }

    public IOUState(Amount<TokenType> amount, Party lender, Party borrower) {
        this(amount, lender, borrower, new Amount<>(0, amount.getToken()), new UniqueIdentifier());
    }

    public Amount<TokenType> getAmount() {
        return amount;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public Amount<TokenType> getPaid() {
        return paid;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     * - [copy] creates a copy of the state using the internal copy constructor ensuring the LinearId is preserved.
     */
    public IOUState pay(Amount<TokenType> amountToPay) {
        Amount<TokenType> newAmountPaid = this.paid.plus(amountToPay);
        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, paid, linearId);
    }

    public IOUState copy(Amount<TokenType> amount, Party lender, Party borrower, Amount<TokenType> paid) {
        return new IOUState(amount, lender, borrower, paid, this.getLinearId());
    }

    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof IOUCustomSchema) {
            return new IOUCustomSchema.PersistentIOU(linearId.getId(), lender.getName().toString(),
                    borrower.getName().toString(), amount.getQuantity());
        } else{
            throw new IllegalArgumentException("Unrecognised schema " + schema);
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new IOUCustomSchema());
    }

}


