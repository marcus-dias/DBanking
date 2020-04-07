package com.marcus.states

import com.marcus.contracts.AccountContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

@BelongsToContract(AccountContract::class)
data class AccountState(
        val walletStateId: UniqueIdentifier,
        val amount: Amount<Currency>,
        val creationDate: Date,
        override val participants: List<AbstractParty> = listOf(),
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    fun copyMinus(amount: Amount<Currency>) = this.copy(amount = amount.minus(amount))
    fun copyPlus(amount: Amount<Currency>) = this.copy(amount = amount.plus(amount))
}

@CordaSerializable
enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
}
