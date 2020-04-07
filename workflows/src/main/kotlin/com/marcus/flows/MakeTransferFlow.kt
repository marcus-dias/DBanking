package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.AccountContract
import com.marcus.contracts.BaseContract
import com.marcus.contracts.TransferContract
import com.marcus.states.AccountState
import com.marcus.states.TransferState
import com.marcus.states.TransferStatus
import com.marcus.states.WalletState
import com.marcus.utils.findAccountForCurrency
import com.marcus.utils.findLedgerState
import com.marcus.utils.findState
import com.marcus.utils.getContractState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap
import java.util.*

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
        val (destinationWalletStateAndRef, destinationAccountStateAndRef) = counterPartySessionFlow
                .sendAndReceive<Pair<StateAndRef<WalletState>, StateAndRef<AccountState>>>(amount.token)
                .unwrap { it }

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
        val buildTransaction = buildTransaction().apply {
            // add inputs
            addInputState(originWalletStateAndRef)
            addInputState(originAccountStateAndRef)
            addInputState(destinationWalletStateAndRef)
            addInputState(destinationAccountStateAndRef)
            // add outputs
            addOutputState(newOriginAccountState, AccountContract.CONTRACT_ID)
            addOutputState(newDestinationAccountState, AccountContract.CONTRACT_ID)
            addOutputState(transferState, TransferContract.CONTRACT_ID)
        }

        val finalisedTransaction = collectSignaturesAndUpdateLedger(buildTransaction, counterPartySessionFlow)

        return findState(finalisedTransaction)
    }

    override fun createCommands(): List<Command<out BaseContract.MyCommand>> {
        return emptyList()
    }
}

@InitiatedBy(MakeTransferFlow::class)
class MakeTransferFlowResponder(private val launcherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val currency = launcherSession.receive<Currency>().unwrap { it }
        val walletStateAndRef = findLedgerState<WalletState>()
        val accountStateAndRef = findAccountForCurrency(currency)
        launcherSession.send(Pair(walletStateAndRef, accountStateAndRef))
    }
}