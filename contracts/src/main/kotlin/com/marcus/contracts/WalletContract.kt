package com.marcus.contracts

import com.marcus.states.AccountState
import com.marcus.states.WalletState
import com.marcus.states.WalletStatus
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class WalletContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = WalletContract::class.qualifiedName!!
    }

    class CreateWalletCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            val walletState = tx.outputStates.filterIsInstance<WalletState>().single()
            val accountStates = tx.outputStates.filterIsInstance<AccountState>()

            require(walletState.status == WalletStatus.ACTIVE) { "A newly created WalletState must be Active" }
            accountStates.forEach {
                require(walletState.linearId == it.walletStateId) {
                    "AccountState must be associated with the WalletState."
                }
                require(it.participants == walletState.participants) {
                    "The owner of WalletState and AccountState must be the same."
                }
            }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
            require(tx.inputStates.isEmpty()) { "Input states must be empty." }
            val outputStates = tx.outputStates
            require(outputStates.isNotEmpty()) { "Output states must not be empty." }
            val walletStates = outputStates.filterIsInstance<WalletState>()
            require(walletStates.isNotEmpty() && walletStates.size == 1) {
                "Output states must have a WalletState."
            }
            val accountStates = outputStates.filterIsInstance<AccountState>()
            require(accountStates.size + walletStates.size == outputStates.size) {
                "We can have only WalletStates and AccountStates."
            }
            require(tx.commands.find { it.value is CreateWalletCommand } != null) {
                "Create Wallet Command must be present."
            }
            if (accountStates.isNotEmpty()) {
                require(tx.commands.any { it.value is AccountContract.CreateAccountWithWalletCommand }) {
                    "Create Account With Wallet Command must be present."
                }
            }
        }

        override fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {
            val walletState = tx.outputStates.filterIsInstance<WalletState>().single()
            require(signers.contains(walletState.owner.owningKey)) { "owner must sign transaction." }
        }
    }
}