package com.template.webserver;

import com.google.common.collect.ImmutableList;
import com.template.flows.IOUIssueFlow;
import com.template.states.IOUState;
import com.template.states.IOUToken;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.core.ExpectKt;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.*;
import net.corda.testing.node.TestCordapp;
import org.junit.Test;
import static net.corda.testing.core.ExpectKt.expect;
import static net.corda.testing.core.ExpectKt.expectEvents;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ApiExercises {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "", "US"));

    /**
     * TODO: Implement the [getIOUs] method of the RPC API [Controller]
     * Hint:
     * - Use the [vaultQuery] RPC method and parameterize by IOUState
     * - You can specify a query criteria or simply pass in a Class reference into the vaultQuery method
     * -- Indicate a class reference in kotlin using the [::class.java] syntax
     */
    @Test
    public void vaultQuery() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true).withCordappsForAllNodes(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.template.contracts"),
                        TestCordapp.findCordapp("com.template.flows")
                )
        ), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                IOUState iou = new IOUState(
                        new Amount(50, new IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()));

                partyBHandle.getRpc().startFlowDynamic(IOUIssueFlow.InitiatorFlow.class, iou).getReturnValue().get();

                List<StateAndRef<IOUState>> result = new Controller(partyAHandle.getRpc()).getIOUs();

                assertEquals(1, result.size());
                assertEquals(50, result.get(0).getState().getData().getAmount().getQuantity());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                fail();
            }

            return null;
        });
    }

    /**
     * TODO: Implement the [getIousWithLinearId] method of the RPC API [Controller]
     * Hint:
     * - Use the [vaultQueryBy] RPC method using a LinearStateQueryCriteria
     * - First, we need to convert the String linear ID into a UniqueIdentifier.
     * -- Use the [fromString] static method from the [UniqueIdentifier] class
     * - Create a [QueryCriteria.LinearStateQueryCriteria] instance that will filter by our linear ID.
     * -- The LinearStateQueryCrtieria takes a list of UUID linear IDs as the second parameter
     * --- Use the [listOf] to create a list on the fly
     * --- You can leave all other constructor parameters null since we don't care to override the default
     * for those fields.
     * - Parameterize the [vaultQueryBy] method by IOUState,
     *      ex) vaultQueryBy<MyState>
     */
    @Test
    public void vaultQueryLinearId() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true).withCordappsForAllNodes(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.template.contracts"),
                        TestCordapp.findCordapp("com.template.flows")
                )
        ), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                IOUState iou = new IOUState(
                        new Amount(50, new IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()));
                IOUState iou2 = new IOUState(
                        new Amount(51, new IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()));

                partyBHandle.getRpc().startFlowDynamic(IOUIssueFlow.InitiatorFlow.class, iou).getReturnValue().get();
                partyBHandle.getRpc().startFlowDynamic(IOUIssueFlow.InitiatorFlow.class, iou2).getReturnValue().get();

                List<StateAndRef<IOUState>> result = new Controller(partyAHandle.getRpc())
                        .getIousWithLinearId(iou.getLinearId().toString());

                assertEquals(1, result.size());
                assertEquals(50, result.get(0).getState().getData().getAmount().getQuantity());

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                fail();
            }

            return null;
        });
    }

//    /**
//     * TODO: Implement the [getIOUsWithAmountGreaterThan] method of the RPC API [Controller]
//     * Hint:
//     * - Use [vaultQueryBy] with a [VaultCustomQueryCriteria] within a [builder] DSL lambda block
//     * - Create a lambda scope by using the [builder] function from [QueryCriteriaUtils]
//     *    ex)
//     *    builder {
//     *      // create criteria using DSL helper functions (greatherThan, lessThan, sum, etc..)
//     *      proxy.vaultQueryBy<IOUState>(myCriteria)
//     *    }
//     * - Within the builder block, create a [VaultCustomQueryCrtieria]
//     * -- Pass in filter function as argument to VaultCustomQueryCritiera
//     *    ex)
//     *    VaultCustomQueryCriteria(
//     *       PersistentCashState::pennies
//     *      .greaterThanOrEqual(10L)
//     *    )
//     * - Call [vaultQueryBy] within the [builder] block passing in our custom criteria object
//     *   ex) vaultQueryBy<IOUState>(criteria)queryBy<IOUState>(criteria)
//     */
    @Test
    public void vaultQueryCustomSchema() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true).withCordappsForAllNodes(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.template.contracts"),
                        TestCordapp.findCordapp("com.template.flows")
                )
        ), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName())),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();

                IOUState iou = new IOUState(
                        new Amount(49, new IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()));
                IOUState iou2 = new IOUState(
                        new Amount(52, new IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()),
                        partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()));

                partyBHandle.getRpc().startFlowDynamic(IOUIssueFlow.InitiatorFlow.class, iou).getReturnValue().get();
                partyBHandle.getRpc().startFlowDynamic(IOUIssueFlow.InitiatorFlow.class, iou2).getReturnValue().get();

                List<StateAndRef<IOUState>> result = new Controller(partyAHandle.getRpc())
                        .getIOUsWithAmountGreaterThan(50L);

                assertEquals(1, result.size());
                assertEquals(52, result.get(0).getState().getData().getAmount().getQuantity());

            } catch (InterruptedException | ExecutionException | NoSuchFieldException e) {
                e.printStackTrace();
                fail();
            }

            return null;
        });
    }
//    @Test
//    fun `vault query custom schema`() = withDriver {
//        // Start a pair of nodes and wait for them both to be ready.
//        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)
//
//        partyBHandle.rpc.startFlow(::IOUIssueFlow,
//                IOUState(Amount(49, IOUToken("IOU_TOKEN", 2)),
//                        partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
//        ).returnValue.getOrThrow()
//
//        partyBHandle.rpc.startFlow(::IOUIssueFlow,
//                IOUState(Amount(52, IOUToken("IOU_TOKEN", 2)),
//                        partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
//        ).returnValue.getOrThrow()
//
//        val result = Controller(partyAHandle.rpc).getIOUsWithAmountGreaterThan(50)!!
//                assertEquals(1, result.size)
//        assertEquals(52, result.get(0).state.data.amount.quantity)
//    }

}

