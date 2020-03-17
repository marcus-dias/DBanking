package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.WalletContract
import com.marcus.states.WalletState
import com.marcus.states.WalletStatus
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@StartableByRPC
@InitiatingFlow
class CreateWalletFlow : FlowLogic<WalletState>() {

    @Suspendable
    override fun call(): WalletState {
        val state = WalletState(ourIdentity, WalletStatus.ACTIVE, Date())
        val transactionBuilder = buildTransaction(state)
        transactionBuilder.verify(serviceHub)
        val signInitialTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        signInitialTransaction.verifyRequiredSignatures()
        val finalisedTransaction = subFlow(FinalityFlow(signInitialTransaction, FinalityFlow.tracker()))
        return finalisedTransaction.tx.outRefsOfType<WalletState>().map { it.state.data }.first()
    }

    private fun buildTransaction(state: WalletState): TransactionBuilder {
        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(state, WalletContract.WALLET_CONTRACT_ID)
        builder.addCommand(createCommand())
        return builder
    }

    private fun createCommand(): Command<WalletContract.CreateWalletCommand> {
        return Command(WalletContract.CreateWalletCommand(), listOf(ourIdentity.owningKey))
    }
}