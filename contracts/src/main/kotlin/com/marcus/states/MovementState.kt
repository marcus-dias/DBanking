package com.marcus.states

import com.marcus.contracts.MovementContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

@BelongsToContract(MovementContract::class)
data class MovementState(
        val originAccountId: UniqueIdentifier,
        val destinationAccountId: UniqueIdentifier,
        val amount: Amount<Currency>,
        val executionDate: Date,
        val status: MovementType,
        override val participants: List<AbstractParty> = listOf()) : ContractState

@CordaSerializable
enum class MovementType {
    CREDIT,
    DEBIT
}
