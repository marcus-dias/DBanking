package com.marcus.states

import com.marcus.contracts.WalletContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * Wallet state used to represent holder of all accounts a owner party has on the network
 */
@BelongsToContract(WalletContract::class)
data class WalletState(
        val owner: Party,
        val status: WalletStatus,
        val creationDate: Date,
        override val participants: List<AbstractParty> = listOf(owner),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState

/**
 * Wallet status possible values
 */
@CordaSerializable
enum class WalletStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
}