package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.BaseContract
import com.marcus.utils.getNotaryNode
import net.corda.core.contracts.Command
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.security.PublicKey

// *********
// * Flows *
// *********
abstract class BaseFlow<T> : FlowLogic<T>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    protected fun buildTransaction(vararg commands: Pair<out BaseContract.MyCommand, List<PublicKey>>) =
            TransactionBuilder(getNotaryNode()).apply {
                commands.forEach { addCommand(Command(it.first, it.second)) }
            }

    @Suspendable
    protected fun collectSignaturesAndUpdateLedger(
            transactionBuilder: TransactionBuilder,
            sessions: List<FlowSession> = emptyList()
    ): SignedTransaction {
        val signedTransaction = collectAllSignatures(transactionBuilder, sessions)
        return updateLedger(signedTransaction, sessions)
    }

    @Suspendable
    protected fun signTransaction(transactionBuilder: TransactionBuilder): SignedTransaction {
        transactionBuilder.verify(serviceHub)
        return serviceHub.signInitialTransaction(transactionBuilder)
    }

    @Suspendable
    protected fun collectAllSignatures(
            transactionBuilder: TransactionBuilder,
            sessions: List<FlowSession> = emptyList()
    ): SignedTransaction {
        val signedTransaction = signTransaction(transactionBuilder)
        if (sessions.isNotEmpty()) {
            return collectOthersSignatures(signedTransaction, sessions)
        }
        signedTransaction.verifyRequiredSignatures()
        return signedTransaction
    }

    @Suspendable
    protected fun collectOthersSignatures(
            signedTransaction: SignedTransaction,
            sessions: List<FlowSession>
    ): SignedTransaction {
        val fullySignedTransaction = subFlow(
                CollectSignaturesFlow(
                        signedTransaction,
                        sessions,
                        CollectSignaturesFlow.tracker()
                )
        )
        fullySignedTransaction.verifySignaturesExcept(getNotaryNode().owningKey)
        return fullySignedTransaction
    }

    @Suspendable
    protected fun updateLedger(signedTransaction: SignedTransaction, sessions: List<FlowSession>): SignedTransaction {
        return subFlow(FinalityFlow(signedTransaction, sessions, FinalityFlow.tracker()))
    }
}


//@InitiatedBy(BaseFlow::class)
//class Responder(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        // Responder flow logic goes here.
//    }
//}
//
