package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.BaseContract
import com.marcus.contracts.WalletContract
import com.marcus.states.WalletState
import com.marcus.states.WalletStatus
import com.marcus.utils.findState
import net.corda.core.contracts.Command
import java.util.*

class CreateWalletFlow : BaseFlow<WalletState>() {

    @Suspendable
    override fun call(): WalletState {
        val state = WalletState(ourIdentity, WalletStatus.ACTIVE, Date())
        val transactionBuilder = buildTransaction()

        transactionBuilder.addOutputState(state, WalletContract.CONTRACT_ID)
        val signInitialTransaction = signTransaction(transactionBuilder)

        signInitialTransaction.verifyRequiredSignatures()

        val finalisedTransaction = updateLedger(signInitialTransaction)
        return findState(finalisedTransaction)
    }

    override fun createCommands(): List<Command<out BaseContract.MyCommand>> {
        return listOf(
                Command(WalletContract.CreateWalletCommand(), listOf(ourIdentity.owningKey))
        )
    }
}