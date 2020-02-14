package com.template.contracts;

import com.google.common.collect.ImmutableList;
import com.template.states.*;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.schemas.QueryableState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import static net.corda.testing.node.NodeTestUtils.ledger;
import org.junit.Test;
import net.corda.core.schemas.*;

public class StateAndContractExercises {
    private final MockServices ledgerServices = new MockServices();

    TestIdentity ALICE = new TestIdentity(new CordaX500Name("Alice", "TestLand","US"));
    TestIdentity BOB = new TestIdentity(new CordaX500Name("Bob", "TestCity","US"));

    /**
     * TODO: Turn the [IOUState] into a [QueryableState].
     * @see IOUState
     * @see IOUCustomSchema
     * @see QueryableState
     * @see MappedSchema
     * @see PersistentState
     * Hint:
     * - [QueryableState] implements [ContractState] and has two additional function requirements.
     * - Update the IOU State to implement the [QueryableState].
     * -- There will be compilation errors until we finish the remaining steps.
     * - Create custom [MappedSchema] and [PersistentState] implementations within [IOUCustomSchema].
     * -- We need to create a custom implementation of the [MappedSchema] class.
     * -- Nest within our custom [MappedSchema] class, we will need a custom implementation of a [PersistentState].
     * -- This uses JPA (Java Persistence API) notation to define how the state will be stored in the database.
     * -- Use the @Entity annotation for our [PersistentState] to define it as a database table enumeration.
     * -- Use the @Column annotation on each field within the [PersistentState] to define the table columns.
     * - Implement the [supportedSchemas] and [generateMappedObject] methods from the [QueryableState] interface.
     * -- [generateMappedObject] takes in a MappedSchema instance and returns the corresponding PersistentState.
     * -- In this way, we could potentially have multiple Schema definitions.
     * -- [supportedSchemas] simply returns a list of schemas supported by this QueryableState.
     */
    @Test
    public void implementQueryableStateOnIOU() {
        assert(QueryableState.class.isAssignableFrom(IOUState.class));
    }

    /**
     * TODO: Implement state grouping for the Merge command.
     * @see IOUContract
     * Hint:
     * - Use State Grouping in the [verify] function of the [IOUContract] to validate a Merge operation.
     * - The [IOUState] has been updated to use a Token from the Token SDK instead of a Currency.
     * We'll learn more about the Token SDK later in the course.
     * - We just need to make sure the the [IOUState]'s [amount.token.tokenIdentifier] field matches for
     * any tokens that are being merged.
     * - First, we need to use the [groupStates] function of the [tx] object.
     * -- [groupStates] takes as argument the [Class] that we are gouping and
     * then and a lambda function that provides the grouping key.
     * ex. groupStates(ClassToGroup.class, (it) -> it.getFieldToUseAsGroupingKey());
     * -- In our case the grouping key will be the [amount.token.tokenIdentifier].
     * -- The return type from the [groupStates] function is a list of [InOutGroup].
     * - Next, we'll loop through the [InOutGroup]s and verify that the total amount of the
     * [inputs] matches that of the [outputs] (i.e. two IOU's of 5 can be merged to 1 IOU of quantity 10).
     */
    @Test
    public void stateGroupingTest() {
        IOUState token1 = new IOUState(new Amount(50, new IOUToken("IOU_TOKEN", 2)), ALICE.getParty(), BOB.getParty());
        IOUState token2 = new IOUState(new Amount(100, new IOUToken("IOU_TOKEN", 2)), ALICE.getParty(), BOB.getParty());
        IOUState token3 = new IOUState(new Amount(100, new IOUToken("DIFFERENT_TOKEN", 2)), ALICE.getParty(), BOB.getParty());
        IOUState token4 = new IOUState(new Amount(200, new IOUToken("DIFFERENT_TOKEN", 2)), ALICE.getParty(), BOB.getParty());

        IOUState output1 = new IOUState(new Amount(150, new IOUToken("IOU_TOKEN", 2)), ALICE.getParty(), BOB.getParty());
        IOUState output2 = new IOUState(new Amount(300, new IOUToken("DIFFERENT_TOKEN", 2)), ALICE.getParty(), BOB.getParty());

        IOUState invalidOutput = new IOUState(new Amount(301, new IOUToken("DIFFERENT_TOKEN", 2)), ALICE.getParty(), BOB.getParty());

        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, token1);
                tx.input(IOUContract.IOU_CONTRACT_ID, token2);
                tx.input(IOUContract.IOU_CONTRACT_ID, token3);
                tx.input(IOUContract.IOU_CONTRACT_ID, token4);
                tx.output(IOUContract.IOU_CONTRACT_ID, output2);
                tx.command(ImmutableList.of(ALICE.getPublicKey(), BOB.getPublicKey()),
                        new IOUContract.Commands.Merge());
                tx.failsWith("Output total must equal input total for each token identifier"); // Not the same token identifier
                return null;
            });
            return null;
        }));
    }

}
