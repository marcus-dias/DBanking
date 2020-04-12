package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.TransferContract
import com.marcus.states.AccountState
import com.marcus.states.TransferState
import com.marcus.states.TransferStatus
import com.marcus.states.WalletState
import com.marcus.utils.findAccountForCurrency
import com.marcus.utils.findMyWallet
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
class RequestTransferFlow(
        private val origin: Party,
        private val amount: Amount<Currency>
) : BaseFlow<TransferState>() {

    @Suspendable
    override fun call(): TransferState {
        // inputs
        val destinationWalletStateAndRef = findMyWallet()
        val destinationAccountStateAndRef = findAccountForCurrency(amount.token)

        val counterPartySessionFlow = initiateFlow(origin)
        counterPartySessionFlow.send(amount.token)

        val originWalletStateAndRef = subFlow(ReceiveStateAndRefFlow<WalletState>(counterPartySessionFlow)).single()
        val originAccountStateAndRef = subFlow(ReceiveStateAndRefFlow<AccountState>(counterPartySessionFlow)).single()

        val originAccountState = originAccountStateAndRef.getContractState()
        val destinationAccountState = destinationAccountStateAndRef.getContractState()

        val transferState = TransferState(
                originAccountState.linearId,
                destinationAccountState.linearId,
                TransferStatus.REQUESTED,
                Date(),
                null,
                amount,
                listOf(ourIdentity, origin)
        )

        // build transaction
        val originKey = origin.owningKey
        val destinationKey = ourIdentity.owningKey
        val buildTransaction = buildTransaction(
                TransferContract.CreateTransferCommand() to listOf(originKey, destinationKey)
        ).apply {
            // add inputs
            addReferenceState(originWalletStateAndRef.referenced())
            addReferenceState(destinationWalletStateAndRef.referenced())
            addInputState(originAccountStateAndRef)
            addInputState(destinationAccountStateAndRef)
            // add outputs
            addOutputState(transferState, TransferContract.CONTRACT_ID)
        }

        val finalisedTransaction = collectSignaturesAndUpdateLedger(buildTransaction, listOf(counterPartySessionFlow))
        return findState(finalisedTransaction)
    }
}

@InitiatedBy(RequestTransferFlow::class)
class RequestTransferFlowResponder(private val launcherSession: FlowSession) : FlowLogic<WireTransaction>() {
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