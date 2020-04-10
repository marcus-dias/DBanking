package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.AccountContract
import com.marcus.contracts.MovementContract
import com.marcus.contracts.TransferContract
import com.marcus.states.*
import com.marcus.utils.findAccountForCurrency
import com.marcus.utils.findLedgerState
import com.marcus.utils.findState
import com.marcus.utils.getContractState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap
import java.util.*

@InitiatingFlow
@StartableByRPC
class MakeTransferFlow(
        private val destination: Party,
        private val amount: Amount<Currency>
) : BaseFlow<TransferState>() {

    @Suspendable
    override fun call(): TransferState {
        // inputs
        val originWalletStateAndRef = findLedgerState<WalletState>()
        val originAccountStateAndRef = findLedgerState<AccountState>()

        val counterPartySessionFlow = initiateFlow(destination)
        counterPartySessionFlow.send(amount.token)

        val destinationWalletStateAndRef = subFlow(ReceiveStateAndRefFlow<WalletState>(counterPartySessionFlow)).single()
        val destinationAccountStateAndRef = subFlow(ReceiveStateAndRefFlow<AccountState>(counterPartySessionFlow)).single()

        val originAccountState = originAccountStateAndRef.getContractState()
        val destinationAccountState = destinationAccountStateAndRef.getContractState()

        // outputs
        val newOriginAccountState = originAccountState.copyMinus(amount)
        val newDestinationAccountState = destinationAccountState.copyPlus(amount)
        val transferState = TransferState(
                originAccountState.linearId,
                destinationAccountState.linearId,
                TransferStatus.SUCCESS,
                Date(),
                Date(),
                amount,
                listOf(ourIdentity, destination)
        )
        val originMovementState = MovementState(
                originAccountState.linearId,
                destinationAccountState.linearId,
                amount,
                Date(),
                MovementType.DEBIT,
                listOf(ourIdentity)
        )
        val destinationMovementState = MovementState(
                destinationAccountState.linearId,
                originAccountState.linearId,
                amount,
                Date(),
                MovementType.CREDIT,
                listOf(destination)
        )
        // build transaction
        val originOwningKey = ourIdentity.owningKey
        val destinationOwningKey = destination.owningKey
        val buildTransaction = buildTransaction(
                TransferContract.CreateTransferCommand() to transferState.participants.map { it.owningKey },
                AccountContract.UpdateAccountCommand() to newOriginAccountState.participants.map { it.owningKey },
                AccountContract.UpdateAccountCommand() to newDestinationAccountState.participants.map { it.owningKey },
                MovementContract.CreateMovementCommand() to originMovementState.participants.map { it.owningKey },
                MovementContract.CreateMovementCommand() to destinationMovementState.participants.map { it.owningKey }
        ).apply {
            // add inputs
            addInputState(originWalletStateAndRef)
            addInputState(originAccountStateAndRef)
            addInputState(destinationWalletStateAndRef)
            addInputState(destinationAccountStateAndRef)
            // add outputs
            addOutputState(newOriginAccountState, AccountContract.CONTRACT_ID)
            addOutputState(newDestinationAccountState, AccountContract.CONTRACT_ID)
            addOutputState(transferState, TransferContract.CONTRACT_ID)
            addOutputState(originMovementState, MovementContract.CONTRACT_ID)
            addOutputState(destinationMovementState, MovementContract.CONTRACT_ID)
        }

        val finalisedTransaction = collectSignaturesAndUpdateLedger(buildTransaction, listOf(counterPartySessionFlow))
        return findState(finalisedTransaction)
    }
}

@InitiatedBy(MakeTransferFlow::class)
class MakeTransferFlowResponder(private val launcherSession: FlowSession) : FlowLogic<WireTransaction>() {
    @Suspendable
    override fun call(): WireTransaction {
        val currency = launcherSession.receive<Currency>().unwrap { it }
        val walletStateAndRef = findLedgerState<WalletState>()
        val accountStateAndRef = findAccountForCurrency(currency)
        subFlow(SendStateAndRefFlow(launcherSession, listOf(walletStateAndRef)))
        subFlow(SendStateAndRefFlow(launcherSession, listOf(accountStateAndRef)))

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(launcherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                Ensuring that the transaction received is the expected type, i.e. has the expected type of inputs and outputs
//                Checking that the properties of the outputs are expected, this is in the absence of integrating reference data sources to facilitate this
//                Checking that the transaction is not incorrectly spending (perhaps maliciously) asset states, as potentially the transaction creator has access to some of signer’s state references
            }
        }
        val signedTransaction = subFlow(signTransactionFlow).id
        // id to verify that we receive the transaction we signed
        return subFlow(ReceiveFinalityFlow(launcherSession, signedTransaction)).tx
    }
}