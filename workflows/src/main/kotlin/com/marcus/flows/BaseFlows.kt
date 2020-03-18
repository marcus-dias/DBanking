package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.BaseContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
abstract class BaseFlow<T> : FlowLogic<T>() {
    override val progressTracker = ProgressTracker()

    abstract fun createCommands(): List<Command<out BaseContract.MyCommand>>

    @Suspendable
    protected fun buildTransaction(): TransactionBuilder {
        val builder = TransactionBuilder(getNotaryNode())
        createCommands().forEach { builder.addCommand(it) }
        return builder
    }

    @Suspendable
    protected fun signTransaction(transactionBuilder: TransactionBuilder): SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    protected fun updateLedger(signInitialTransaction: SignedTransaction) =
            subFlow(FinalityFlow(signInitialTransaction, FinalityFlow.tracker()))

    private fun getNotaryNode() = serviceHub.networkMapCache.notaryIdentities.first()

    protected inline fun <reified S : ContractState> findState(finalisedTransaction: SignedTransaction) =
            finalisedTransaction.tx.outRefsOfType<S>().map { it.state.data }.first()

    protected inline fun <reified S : ContractState> findLedgerState(): StateAndRef<S> {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return serviceHub.vaultService.queryBy<S>(criteria).states.first()
    }
}

@InitiatedBy(BaseFlow::class)
class Responder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
