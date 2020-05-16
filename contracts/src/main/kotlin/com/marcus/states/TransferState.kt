package com.marcus.states

import com.marcus.Balance
import com.marcus.contracts.TransferContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * Transfer State used to represent a transfer of a certain amount between two accounts
 */
@BelongsToContract(TransferContract::class)
data class TransferState(
        val originAccountId: UniqueIdentifier,
        val destinationAccountId: UniqueIdentifier,
        val status: TransferStatus,
        val creationDate: Date,
        val executionDate: Date?,
        val amount: Balance<Currency>,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState

/**
 * Transfer status possible values
 */
@CordaSerializable
enum class TransferStatus {
    SUCCESS,
    REQUESTED,
    ERROR
}