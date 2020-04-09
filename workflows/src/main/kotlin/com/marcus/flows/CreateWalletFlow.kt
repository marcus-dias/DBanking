package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.WalletContract
import com.marcus.states.WalletState
import com.marcus.states.WalletStatus
import com.marcus.utils.findState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateWalletFlow : BaseFlow<WalletState>() {

    @Suspendable
    override fun call(): WalletState {
        val state = WalletState(ourIdentity, WalletStatus.ACTIVE, Date())
        val transactionBuilder = buildTransaction(
                WalletContract.CreateWalletCommand() to listOf(ourIdentity.owningKey)
        )
        transactionBuilder.addOutputState(state, WalletContract.CONTRACT_ID)
        val finalisedTransaction = collectSignaturesAndUpdateLedger(transactionBuilder)
        return findState(finalisedTransaction)
    }
}