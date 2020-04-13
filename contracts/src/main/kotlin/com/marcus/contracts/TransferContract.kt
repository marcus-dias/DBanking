package com.marcus.contracts

import net.corda.core.transactions.LedgerTransaction

class TransferContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = TransferContract::class.qualifiedName!!
    }

    class CreateTransferCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }

    class RequestTransferCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }

    class ExecuteRequestedTransferCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }
}