package com.marcus.states

import com.marcus.contracts.WalletContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@BelongsToContract(WalletContract::class)
data class WalletState(
        val owner: Party,
        val status: WalletStatus,
        val creationDate: Date,
        override val participants: List<AbstractParty> = listOf(owner),
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState

@CordaSerializable
enum class WalletStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
}
