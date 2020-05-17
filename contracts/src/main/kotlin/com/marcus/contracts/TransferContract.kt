package com.marcus.contracts

import com.marcus.Balance
import com.marcus.states.AccountState
import com.marcus.states.MovementState
import com.marcus.states.TransferState
import com.marcus.states.TransferStatus
import net.corda.core.contracts.Amount
import net.corda.core.transactions.LedgerTransaction

class TransferContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = TransferContract::class.qualifiedName!!
    }

    class MakeTransferCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            val inputAccountStates = tx.inputStates.filterIsInstance<AccountState>()
            val outputAccountStates = tx.outputStates.filterIsInstance<AccountState>()
            val outputTransferState = tx.outputStates.filterIsInstance<TransferState>().single()
            val outputMovementStates = tx.outputStates.filterIsInstance<MovementState>()

            require(outputTransferState.creationDate == outputTransferState.executionDate) {
                "Make transfer should be executed as soon as it is created."
            }
            require(outputTransferState.amount > Amount.zero(outputTransferState.amount.token)) {
                "Transfer amount must be positive."
            }
            require(outputTransferState.status == TransferStatus.SUCCESS) {
                "Make Transfer is executed as soon as it is created, hence status is success."
            }
            val inputOriginAccount = inputAccountStates.find { it.linearId == outputTransferState.originAccountId }!!
            val inputDestinationAccount = inputAccountStates.find { it.linearId == outputTransferState.destinationAccountId }!!
            val outputOriginAccount = outputAccountStates.find { it.linearId == outputTransferState.originAccountId }!!
            val outputDestinationAccount = outputAccountStates.find { it.linearId == outputTransferState.destinationAccountId }!!
            val originMovement = outputMovementStates.find { it.myAccountId == outputTransferState.originAccountId }!!
            val destinationMovement = outputMovementStates.find { it.myAccountId == outputTransferState.destinationAccountId }!!

            val transferredBalance = Balance.fromAmount(outputTransferState.amount)
            require(inputOriginAccount.balance - outputOriginAccount.balance == transferredBalance) {
                "Origin balance difference should be equals the the amount transferred."
            }
            require(outputDestinationAccount.balance - inputDestinationAccount.balance == transferredBalance) {
                "Destination balance difference should be equals the the amount transferred."
            }
            require(inputOriginAccount.linearId == originMovement.myAccountId) {
                "Origin movement should have origin account id associated."
            }
            require(inputDestinationAccount.linearId == destinationMovement.myAccountId) {
                "Destination movement should have destination account id associated."
            }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
            require(tx.inputStates.isNotEmpty()) { "InputStates must not be empty." }
            require(tx.outputStates.isNotEmpty()) { "InputStates must not be empty." }
            require(tx.inputStates.size == 2) { "There must be two input states." }
            require(tx.outputStates.size == 5) { "There must be five output states." }
            val inputAccountStates = tx.inputStates.filterIsInstance<AccountState>()
            val outputAccountStates = tx.outputStates.filterIsInstance<AccountState>()
            val outputTransferStates = tx.outputStates.filterIsInstance<TransferState>()
            val outputMovementStates = tx.outputStates.filterIsInstance<MovementState>()
            require(inputAccountStates.size == 2) { "There must be two accounts input states." }
            require(outputAccountStates.size == 2) { "There must be two accounts input states." }
            require(inputAccountStates.map { it.linearId } == outputAccountStates.map { it.linearId }) {
                "Different accounts on input and output."
            }
            require(outputTransferStates.size == 1) { "There should be only one TransferState." }
            require(outputMovementStates.size == 2) { "There should be two MovementStates." }
            inputAccountStates.map { it.linearId }.forEach { id ->
                require(outputMovementStates.find { it.myAccountId == id } != null) {
                    "There should be a MovementState for every account associated with this transfer."
                }
            }
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