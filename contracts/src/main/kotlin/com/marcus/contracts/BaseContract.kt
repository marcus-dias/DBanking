package com.marcus.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

// ************
// * Contract *
// ************
abstract class BaseContract : Contract {
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        require(tx.notary != null) { "There must be a notary." }
        tx.commands.forEach {
            (it.value as MyCommand).apply {
                verifyContractShape(tx)
                verifyContractSpecifics(tx)
                verifySignatures(tx, it.signers)
            }
        }
    }

    // Used to indicate the transaction's intent.
    abstract class MyCommand : CommandData {
        open fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {}
        abstract fun verifyContractSpecifics(tx: LedgerTransaction)
        abstract fun verifyContractShape(tx: LedgerTransaction)
    }
}