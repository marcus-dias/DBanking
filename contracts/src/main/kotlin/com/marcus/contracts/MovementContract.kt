package com.marcus.contracts

import com.marcus.states.MovementState
import com.marcus.states.MovementType
import com.marcus.states.TransferState
import net.corda.core.contracts.Amount
import net.corda.core.transactions.LedgerTransaction

class MovementContract : BaseContract() {
    companion object {
        @JvmStatic
        val CONTRACT_ID = MovementContract::class.qualifiedName!!
    }

    class CreateMovementCommand : MyCommand() {

        override fun verifyContractSpecifics(tx: LedgerTransaction) {
            val transferState = tx.outputStates.filterIsInstance<TransferState>().single()
            val outputMovementStates = tx.outputStates.filterIsInstance<MovementState>()
            val originMovement = outputMovementStates.find { it.myAccountId == transferState.originAccountId }!!
            val destinationMovement = outputMovementStates.find { it.myAccountId == transferState.destinationAccountId }!!

            require(originMovement.amount == transferState.amount) {
                "Movement amount and transfer amount should be the same."
            }
            require(destinationMovement.amount == transferState.amount) {
                "Movement amount and transfer amount should be the same."
            }
            require(originMovement.status == MovementType.DEBIT) { "Origin status should be debit." }
            require(destinationMovement.status == MovementType.CREDIT) { "Destination status should be credit." }
            require(originMovement.amount != Amount.zero(originMovement.amount.token)) { "Amount should not be zero." }
            require(destinationMovement.amount != Amount.zero(destinationMovement.amount.token)) {
                "Amount should not be zero."
            }
            require(originMovement.amount == destinationMovement.amount) {
                "Moved amount should be the same."
            }
            require(originMovement.myAccountId == destinationMovement.counterAccountId) {
                "Origin account should be the counter account from destination."
            }
            require(originMovement.counterAccountId == destinationMovement.myAccountId) {
                "Destination account should be the counter account from origin."
            }
            require(originMovement.executionDate == transferState.executionDate &&
                    destinationMovement.executionDate == transferState.executionDate) {
                "There should be only one execution date for all movements and transfer."
            }
            require(originMovement.transferId == transferState.linearId &&
                    destinationMovement.transferId == transferState.linearId) {
                "Origin movement and destination movement should be associated with the transfer by ID."
            }
        }

        override fun verifyContractShape(tx: LedgerTransaction) {
            require(tx.outputStates.filterIsInstance<MovementState>().size == 2) {
                "There should always be two MovementState."
            }
            require(tx.outputStates.filterIsInstance<TransferState>().size == 1) {
                "Movements can only be created associated with a transfer."
            }
        }
    }
}