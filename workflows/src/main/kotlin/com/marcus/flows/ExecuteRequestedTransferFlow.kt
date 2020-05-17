package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.Balance
import com.marcus.contracts.AccountContract
import com.marcus.contracts.MovementContract
import com.marcus.contracts.TransferContract
import com.marcus.states.*
import com.marcus.utils.*
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap
import java.util.*

@InitiatingFlow
@StartableByRPC
class ExecuteRequestedTransferFlow(
        private val linearId: UniqueIdentifier
) : BaseFlow<TransferState>() {

    @Suspendable
    override fun call(): TransferState {
        // inputs
        val transferStateAndRef = findLedgerStateById<TransferState>(linearId)
        val transferState = transferStateAndRef.getContractState()
        val amount = transferState.amount

        val originWalletStateAndRef = findMyWallet()
        originWalletStateAndRef.requireIsActive()

        val originAccountStateAndRef = findLedgerStateById<AccountState>(transferState.originAccountId)

        val destination = findPartyByPublicKey((transferState.participants - ourIdentity).single().owningKey)
        val counterPartySessionFlow = initiateFlow(destination)

        counterPartySessionFlow.send(amount.token)
        val destinationWalletStateAndRef = subFlow(ReceiveStateAndRefFlow<WalletState>(counterPartySessionFlow)).single()
        destinationWalletStateAndRef.requireIsActive()

        val destinationAccountStateAndRef = subFlow(ReceiveStateAndRefFlow<AccountState>(counterPartySessionFlow)).single()

        val originAccountState = originAccountStateAndRef.getContractState()
        val destinationAccountState = destinationAccountStateAndRef.getContractState()

        // outputs
        val balance = Balance.fromAmount(amount)
        val newOriginAccountState = originAccountState.copyMinus(balance)
        val newDestinationAccountState = destinationAccountState.copyPlus(balance)
        val transferDate = Date()
        val newTransferState = transferState.copy(executionDate = transferDate, status = TransferStatus.SUCCESS)
        val originMovementState = MovementState(
                newTransferState.linearId,
                originAccountState.linearId,
                destinationAccountState.linearId,
                amount,
                transferDate,
                MovementType.DEBIT,
                listOf(ourIdentity)
        )
        val destinationMovementState = MovementState(
                newTransferState.linearId,
                destinationAccountState.linearId,
                originAccountState.linearId,
                amount,
                transferDate,
                MovementType.CREDIT,
                listOf(destination)
        )
        // build transaction
        val originKey = ourIdentity.owningKey
        val destinationKey = destination.owningKey
        val buildTransaction = buildTransaction(
                TransferContract.ExecuteRequestedTransferCommand() to listOf(originKey, destinationKey),
                AccountContract.TransferAccountsBalancesCommand() to listOf(originKey, destinationKey),
                MovementContract.CreateMovementCommand() to listOf(originKey, destinationKey)
        ).apply {
            // add inputs
            addInputState(originAccountStateAndRef)
            addInputState(destinationAccountStateAndRef)
            addInputState(transferStateAndRef)
            // add outputs
            addOutputState(newOriginAccountState, AccountContract.CONTRACT_ID)
            addOutputState(newDestinationAccountState, AccountContract.CONTRACT_ID)
            addOutputState(newTransferState, TransferContract.CONTRACT_ID)
            addOutputState(originMovementState, MovementContract.CONTRACT_ID)
            addOutputState(destinationMovementState, MovementContract.CONTRACT_ID)
        }

        val finalisedTransaction = collectSignaturesAndUpdateLedger(buildTransaction, listOf(counterPartySessionFlow))
        return findState(finalisedTransaction)
    }
}

@InitiatedBy(ExecuteRequestedTransferFlow::class)
class ExecuteRequestedTransferFlowResponder(private val launcherSession: FlowSession) : FlowLogic<WireTransaction>() {
    @Suspendable
    override fun call(): WireTransaction {
        val currency = launcherSession.receive<Currency>().unwrap { it }
        val walletStateAndRef = findMyWallet()
        val accountStateAndRef = findAccountForCurrency(currency)
        subFlow(SendStateAndRefFlow(launcherSession, listOf(walletStateAndRef)))
        subFlow(SendStateAndRefFlow(launcherSession, listOf(accountStateAndRef)))

        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(launcherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }
        val signedTransaction = subFlow(signTransactionFlow).id
        // id to verify that we receive the transaction we signed
        return subFlow(ReceiveFinalityFlow(launcherSession, signedTransaction)).tx
    }
}