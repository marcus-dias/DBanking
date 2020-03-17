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
        tx.commands.forEach {
            val myCommand = it.value as MyCommand
            myCommand.verifyContractShape(tx)
            myCommand.verifyContractSpecifics(tx)
            myCommand.verifySignatures(tx, it.signers)
        }
    }

    // Used to indicate the transaction's intent.
    abstract class MyCommand : CommandData {
        fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {

        }

        abstract fun verifyContractSpecifics(tx: LedgerTransaction)
        abstract fun verifyContractShape(tx: LedgerTransaction)
    }
}