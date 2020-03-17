package com.marcus.contracts

import net.corda.core.transactions.LedgerTransaction

class WalletContract : BaseContract() {
    companion object {
        @JvmStatic
        val WALLET_CONTRACT_ID = WalletContract::class.qualifiedName!!
    }

    class CreateWalletCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }
}