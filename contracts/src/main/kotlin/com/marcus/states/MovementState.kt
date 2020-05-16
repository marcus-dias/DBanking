package com.marcus.states

import com.marcus.Balance
import com.marcus.contracts.MovementContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * Movement state, is the result of a transfer between two accounts.
 * after an transfer is executed, there is two movements of a certain amount created
 * one for the origin account
 * one for the destination account
 * with proper movement types
 */
@BelongsToContract(MovementContract::class)
data class MovementState(
        val myAccountId: UniqueIdentifier,
        val counterAccountId: UniqueIdentifier,
        val amount: Balance<Currency>,
        val executionDate: Date,
        val status: MovementType,
        override val participants: List<AbstractParty>
) : ContractState

/**
 * Movement Type possible values
 */
@CordaSerializable
enum class MovementType {
    CREDIT,
    DEBIT
}