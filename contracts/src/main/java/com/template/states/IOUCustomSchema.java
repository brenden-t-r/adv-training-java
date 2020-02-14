package com.template.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;


public class IOUCustomSchema {

    public IOUCustomSchema() {

    }

    @Entity
    @Table
    public static class PersistentIOU {

    }
}
