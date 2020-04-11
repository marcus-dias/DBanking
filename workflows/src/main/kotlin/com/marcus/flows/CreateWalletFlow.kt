package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.Balance
import com.marcus.contracts.AccountContract
import com.marcus.contracts.WalletContract
import com.marcus.states.AccountState
import com.marcus.states.WalletState
import com.marcus.states.WalletStatus
import com.marcus.utils.findState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateWalletFlow(
        private val accountCurrencies: Set<Currency>
) : BaseFlow<WalletState>() {

    @Suspendable
    override fun call(): WalletState {
        val state = WalletState(ourIdentity, WalletStatus.ACTIVE, Date())
        val accountStates = accountCurrencies.map {
            AccountState(
                    state.linearId,
                    Balance.zero(it),
                    Date(),
                    listOf(ourIdentity)
            )
        }
        val transactionBuilder = buildTransaction(
                WalletContract.CreateWalletCommand() to listOf(ourIdentity.owningKey),
                AccountContract.CreateAccountCommand() to listOf(ourIdentity.owningKey)
        ).apply {
            addOutputState(state, WalletContract.CONTRACT_ID)
            accountStates.forEach {
                addOutputState(it, AccountContract.CONTRACT_ID)
            }
        }
        val finalisedTransaction = collectSignaturesAndUpdateLedger(transactionBuilder)
        return findState(finalisedTransaction)
    }
}