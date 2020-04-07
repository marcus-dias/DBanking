package com.marcus.contracts

import net.corda.core.transactions.LedgerTransaction

class AccountContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = AccountContract::class.qualifiedName!!
    }

    class CreateAccountCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }
}