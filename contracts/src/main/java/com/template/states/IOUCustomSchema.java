package com.template.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;


public class IOUCustomSchema extends MappedSchema {

    public IOUCustomSchema() {
        super(IOUCustomSchema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table
    public static class PersistentIOU extends PersistentState {
        @Column(nullable = false) UUID linearId;
        @Column(nullable = false) String lender;
        @Column(nullable = false) String borrower;
        @Column(nullable = false) Long amount;
        @Column(nullable = false) Boolean settled;

        public PersistentIOU(UUID linearId, String lender, String borrower, Long amount, Boolean settled) {
            this.linearId = linearId;
            this.lender = lender;
            this.borrower = borrower;
            this.amount = amount;
            this.settled = settled;
        }

        public PersistentIOU() {
            this.linearId = null;
            this.lender = null;
            this.borrower = null;
            this.amount = 0L;
            this.settled = false;
        }

        public UUID getLinearId() {
            return linearId;
        }

        public String getLender() {
            return lender;
        }

        public String getBorrower() {
            return borrower;
        }

        public Long getAmount() {
            return amount;
        }

        public Boolean getSettled() {
            return settled;
        }
    }
}
