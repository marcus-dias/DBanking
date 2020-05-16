package com.marcus.contracts

import com.marcus.states.AccountState
import com.marcus.states.AccountStatus
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class AccountContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = AccountContract::class.qualifiedName!!
    }

    class CreateAccountCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            val accountState = tx.outputStates.filterIsInstance<AccountState>().single()
            require(accountState.amount.quantity == 0L) { "AccountState initial balance must be 0." }
            require(accountState.state == AccountStatus.ACTIVE) { "AccountState must be active." }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
            require(tx.notary != null) { "There must be a notary." }
            require(tx.inputStates.isEmpty()) { "InputStates must be empty." }
            require(tx.outputStates.isNotEmpty()) { "OutputStates must not be empty." }
            require(tx.outputStates.size == 1) { "There should be only ONE output state." }
            val accountState = tx.outputStates.filterIsInstance<AccountState>()
            require(accountState.size == 1) { "There should be only ONE AccountState." }
        }

        override fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {
            val accountState = tx.outputStates.filterIsInstance<AccountState>().single()
            require(signers.contains(accountState.participants.single().owningKey)) { "owner must sign transaction." }
        }
    }

    class CreateAccountWithWalletCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            tx.inputStates.filterIsInstance<AccountState>().forEach {
                require(it.amount.quantity == 0L) { "AccountState initial balance must be 0." }
                require(it.state == AccountStatus.ACTIVE) { "AccountState must be active." }
            }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
        }

        override fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {
            tx.outputStates.filterIsInstance<AccountState>().forEach {
                require(signers.contains(it.participants.single().owningKey)) { "owner must sign transaction." }
            }
        }
    }

    class UpdateAccountCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {

        }

        override fun verifyContractShape(tx: LedgerTransaction) {

        }
    }
}