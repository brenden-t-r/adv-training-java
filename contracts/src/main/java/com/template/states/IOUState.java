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
public class IOUState implements LinearState {

    public final Amount<TokenType> amount;
    public final Party lender;
    public final Party borrower;
    private final UniqueIdentifier linearId;
    private final Double novatedAmount;
    private final String novatedCurrency;
    private final String settlementAccount;
    private final Boolean settled;


    // Private constructor used only for copying a State object
    @ConstructorForDeserialization
    public IOUState(Amount<TokenType> amount, Party lender, Party borrower, UniqueIdentifier linearId, Double novatedAmount, String novatedCurrency, String settlementAccount, Boolean settled) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
        this.novatedAmount = novatedAmount;
        this.novatedCurrency = novatedCurrency;
        this.settlementAccount = settlementAccount;
        this.settled = settled;
    }

    public IOUState(Amount<TokenType> amount, Party lender, Party borrower) {
        this(amount, lender, borrower, new UniqueIdentifier(), 0.0, null, null, false);
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

    public Double getNovatedAmount() {
        return novatedAmount;
    }

    public String getNovatedCurrency() {
        return novatedCurrency;
    }

    public Boolean getSettled() {
        return settled;
    }

    public String getSettlementAccount() {
        return settlementAccount;
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


    public IOUState withNovatedAmount(String currency, Double novatedAmount) {
        return new IOUState(amount, lender, borrower, linearId, novatedAmount, currency, settlementAccount, settled);
    }

    public IOUState withSettlementAccount(String settlementAccount) {
        return new IOUState(amount, lender, borrower, linearId, novatedAmount, novatedCurrency, settlementAccount, settled);
    }


    public IOUState withSettled() {
        return new IOUState(amount, lender, borrower, linearId, novatedAmount, novatedCurrency, settlementAccount, true);
    }

    public IOUState withNewLender(Party newLender) {
        return new IOUState(amount, newLender, borrower, linearId, novatedAmount, novatedCurrency, settlementAccount,settled);
    }

//    @Override
//    public PersistentState generateMappedObject(MappedSchema schema) {
//        if (schema instanceof IOUCustomSchema) {
//            return new IOUCustomSchema.PersistentIOU(linearId.getId(), lender.getName().toString(),
//                    borrower.getName().toString(), amount.getQuantity());
//        } else{
//            throw new IllegalArgumentException("Unrecognised schema " + schema);
//        }
//    }
//
//    @Override
//    public Iterable<MappedSchema> supportedSchemas() {
//        return ImmutableList.of(new IOUCustomSchema());
//    }

}


