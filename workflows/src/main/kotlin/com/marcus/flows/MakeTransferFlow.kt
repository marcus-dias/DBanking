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
import net.corda.core.flows.*
import net.corda.core.identity.Party
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
        val buildTransaction = buildTransaction(
                TransferContract.CreateTransferCommand() to listOf(ourIdentity.owningKey, destination.owningKey),
                AccountContract.UpdateAccountCommand() to listOf(ourIdentity.owningKey, destination.owningKey),
                MovementContract.CreateMovementCommand() to listOf(ourIdentity.owningKey, destination.owningKey)
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
class MakeTransferFlowResponder(private val launcherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val currency = launcherSession.receive<Currency>().unwrap { it }
        val walletStateAndRef = findLedgerState<WalletState>()
        val accountStateAndRef = findAccountForCurrency(currency)
        subFlow(SendStateAndRefFlow(launcherSession, listOf(walletStateAndRef)))
        subFlow(SendStateAndRefFlow(launcherSession, listOf(accountStateAndRef)))
    }
}