package com.template.contracts;

import com.template.states.IOUState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.LedgerTransaction.InOutGroup;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IOUContract implements Contract {
    public static final String IOU_CONTRACT_ID = "com.template.contracts.IOUContract";

    /**
     * The IOUContract can handle three transaction types involving [IOUState]s.
     * - Issuance: Issuing a new [IOUState] on the ledger, which is a bilateral agreement between two parties.
     * - Transfer: Re-assigning the lender/beneficiary.
     * - Settle: Fully or partially settling the [IOUState] using the Corda [Cash] contract.
     */
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands{}
        class Transfer extends TypeOnlyCommandData implements Commands{}
        class Merge extends TypeOnlyCommandData implements Commands{}

        class Exchange implements Commands{
            String currency;
            Double rate;
            public Exchange(String currency, Double rate) {
                this.currency = currency;
                this.rate = rate;
            }

            public String getCurrency() {
                return currency;
            }

            public Double getRate() {
                return rate;
            }
        }
    }
    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        // We can use the requireSingleCommand function to extract command data from transaction.
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        /**
         * This command data can then be used inside of a conditional statement to indicate which set of tests we
         * should be performing - we will use different assertions to enable the contract to verify the transaction
         * for issuing, settling and transferring.
         */
        if (commandData.equals(new Commands.Issue())) {

            requireThat(require -> {

                require.using("No inputs should be consumed when issuing an IOU.", tx.getInputStates().size() == 0);
                require.using( "Only one output state should be created when issuing an IOU.", tx.getOutputStates().size() == 1);

                IOUState outputState = tx.outputsOfType(IOUState.class).get(0);
                require.using( "A newly issued IOU must have a positive amount.", outputState.amount.getQuantity() > 0);
                require.using( "The lender and borrower cannot have the same identity.", outputState.lender.getOwningKey() != outputState.borrower.getOwningKey());

                List<PublicKey> signers = tx.getCommands().get(0).getSigners();
                HashSet<PublicKey> signersSet = new HashSet<>();
                for (PublicKey key: signers) {
                    signersSet.add(key);
                }

                List<AbstractParty> participants = tx.getOutputStates().get(0).getParticipants();
                HashSet<PublicKey> participantKeys = new HashSet<>();
                for (AbstractParty party: participants) {
                    participantKeys.add(party.getOwningKey());
                }

                require.using("Both lender and borrower together only may sign IOU issue transaction.", signersSet.containsAll(participantKeys) && signersSet.size() == 2);

                return null;
            });

        }

        else if (commandData.equals(new Commands.Transfer())) {

            requireThat(require -> {

                require.using("An IOU transfer transaction should only consume one input state.", tx.getInputStates().size() == 1);
                require.using("An IOU transfer transaction should only create one output state.", tx.getOutputStates().size() == 1);

                // Copy of input with new lender;
                IOUState inputState = tx.inputsOfType(IOUState.class).get(0);
                IOUState outputState = tx.outputsOfType(IOUState.class).get(0);
                IOUState checkOutputState = outputState.withNewLender(inputState.getLender());

                require.using("Only the lender property may change.",
                        checkOutputState.amount.equals(inputState.amount) && checkOutputState.getLinearId().equals(inputState.getLinearId()) && checkOutputState.borrower.equals(inputState.borrower) && checkOutputState.paid.equals(inputState.paid));
                require.using("The lender property must change in a transfer.", !outputState.lender.getOwningKey().equals(inputState.lender.getOwningKey()));

                List<PublicKey> listOfPublicKeys = new ArrayList<>();
                listOfPublicKeys.add(inputState.lender.getOwningKey());
                listOfPublicKeys.add(inputState.borrower.getOwningKey());
                listOfPublicKeys.add(checkOutputState.lender.getOwningKey());

                Set<PublicKey> listOfParticipantPublicKeys = inputState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toSet());
                listOfParticipantPublicKeys.add(outputState.lender.getOwningKey());
                List<PublicKey> arrayOfSigners = command.getSigners();
                Set<PublicKey> setOfSigners = new HashSet<PublicKey>(arrayOfSigners);
                require.using("The borrower, old lender and new lender only must sign an IOU transfer transaction", setOfSigners.equals(listOfParticipantPublicKeys) && setOfSigners.size() == 3);
                return null;

            });
        }

        else if (commandData.equals(new Commands.Merge())) {

            requireThat(require -> {

                List<InOutGroup<IOUState, String>> inOutGroups = tx
                        .groupStates(IOUState.class, (it) ->
                                it.getAmount().getToken().getTokenIdentifier());

                for (InOutGroup<IOUState, String> inOutGroup : inOutGroups) {
                    inOutGroup.getGroupingKey();
                    inOutGroup.getInputs();
                    inOutGroup.getOutputs();

                    Long inputTotal = 0L;
                    Long outputTotal = 0L;
                    for (IOUState input : inOutGroup.getInputs()) {
                        inputTotal += input.getAmount().getQuantity();
                    }
                    for (IOUState output : inOutGroup.getOutputs()) {
                        outputTotal += output.getAmount().getQuantity();
                    }

                    require.using("Output total must equal input total for each token identifier", (inputTotal == outputTotal));
                }

                return null;

            });

        }
    }

}
