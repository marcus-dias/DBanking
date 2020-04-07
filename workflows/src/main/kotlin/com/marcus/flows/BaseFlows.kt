package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.BaseContract
import com.marcus.utils.getNotaryNode
import net.corda.core.contracts.Command
import net.corda.core.flows.*
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
    protected fun collectSignaturesAndUpdateLedger(
            transactionBuilder: TransactionBuilder,
            vararg sessions: FlowSession): SignedTransaction {
        val signedTransaction = collectAllSignatures(transactionBuilder, *sessions)
        return if (sessions.isEmpty()) {
            updateLedger(signedTransaction)
        } else {
            updateLedger(signedTransaction, *sessions)
        }
    }

    @Suspendable
    protected fun signTransaction(transactionBuilder: TransactionBuilder): SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    protected fun collectAllSignatures(
            transactionBuilder: TransactionBuilder,
            vararg sessions: FlowSession
    ): SignedTransaction {
        val signedTransaction = signTransaction(transactionBuilder)
        if (sessions.isNotEmpty()) {
            return collectOthersSignatures(signedTransaction, *sessions)
        }
        return signedTransaction
    }

    @Suspendable
    protected fun collectOthersSignatures(
            signedTransaction: SignedTransaction,
            vararg sessions: FlowSession
    ): SignedTransaction {
        val fullySignedTransaction = subFlow(
                CollectSignaturesFlow(
                        signedTransaction,
                        sessions.toList(),
                        CollectSignaturesFlow.tracker()
                )
        )
        fullySignedTransaction.verifyRequiredSignatures()
        return fullySignedTransaction
    }

    @Suspendable
    protected fun updateLedger(signedTransaction: SignedTransaction) =
            subFlow(FinalityFlow(signedTransaction, FinalityFlow.tracker()))

    @Suspendable
    protected fun updateLedger(signedTransaction: SignedTransaction, vararg sessions: FlowSession) =
            subFlow(FinalityFlow(signedTransaction, sessions.toList()))
}

@InitiatedBy(BaseFlow::class)
class Responder(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}

