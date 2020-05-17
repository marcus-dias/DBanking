package com.marcus.contracts

import com.marcus.Balance
import com.marcus.states.AccountState
import com.marcus.states.AccountStatus
import com.marcus.states.TransferState
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
            require(accountState.balance.quantity == 0L) { "AccountState initial balance must be 0." }
            require(accountState.state == AccountStatus.ACTIVE) { "AccountState must be active." }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
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
                require(it.balance.quantity == 0L) { "AccountState initial balance must be 0." }
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

    class TransferAccountsBalancesCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            val inputAccountStates = tx.inputStates.filterIsInstance<AccountState>()
            val outputAccountStates = tx.outputStates.filterIsInstance<AccountState>()
            val transferState = tx.outputStates.filterIsInstance<TransferState>().single()
            val amountTransacted = transferState.amount
            inputAccountStates.forEach { input ->
                val output = outputAccountStates.find { it.linearId == input.linearId }!!
                require(input.state == AccountStatus.ACTIVE) { "Balance can only be updated on active accounts." }
                require(output.state == AccountStatus.ACTIVE) { "Balance can only be updated on active accounts." }
                require(input.balance.token == output.balance.token) { "Account currency cannot change." }
                require(input.walletStateId == output.walletStateId) { "Account associated wallet cannot change." }
                require(input.creationDate == output.creationDate) { "Account creation date cannot change." }
                require((output.balance - input.balance + Balance.fromAmount(amountTransacted)) == Balance.zero(amountTransacted.token))
            }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
            require(tx.inputStates.isNotEmpty()) { "InputStates must not be empty." }
            require(tx.outputStates.isNotEmpty()) { "OutputStates must not be empty." }
            require(tx.outputStates.filterIsInstance<TransferState>().size == 1) {
                "Account balance updates must be associated with a TransferState."
            }
            val inputAccountStates = tx.inputStates.filterIsInstance<AccountState>()
            val outputAccountStates = tx.outputStates.filterIsInstance<AccountState>()
            require(inputAccountStates.size == 2) { "We only support amount transfer between 2 accounts." }
            require(outputAccountStates.size == 2) { "We only support amount transfer between 2 accounts." }
            require(inputAccountStates.size == outputAccountStates.size) {
                "The number of accounts on input must be the same as on the output."
            }
            val inputLinearIds = inputAccountStates.map { it.linearId }
            val outputLinearIds = outputAccountStates.map { it.linearId }
            require(inputLinearIds == outputLinearIds) {
                "Different accounts on input and output."
            }
        }

        override fun verifySignatures(tx: LedgerTransaction, signers: List<PublicKey>) {
            tx.outputStates.filterIsInstance<AccountState>().forEach {
                require(signers.contains(it.participants.single().owningKey)) { "owner must sign transaction." }
            }
        }
    }
}