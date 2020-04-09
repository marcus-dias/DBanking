package com.marcus.contracts

import net.corda.core.transactions.LedgerTransaction

class MovementContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = MovementContract::class.qualifiedName!!
    }

    class CreateMovementCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }
}