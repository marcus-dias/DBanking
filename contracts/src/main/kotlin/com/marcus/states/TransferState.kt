package com.marcus.states

import com.marcus.contracts.TransferContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

@BelongsToContract(TransferContract::class)
class TransferState(
        val originAccountId: UniqueIdentifier,
        val destinationAccountId: UniqueIdentifier,
        val status: TransferStatus,
        val creationDate: Date,
        val executionDate: Date,
        val amount: Amount<Currency>,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState

@CordaSerializable
enum class TransferStatus {
    SUCCESS,
    REQUESTED,
    ERROR
}
