package com.marcus.states

import com.marcus.Balance
import com.marcus.contracts.AccountContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

@BelongsToContract(AccountContract::class)
data class AccountState(
        val walletStateId: UniqueIdentifier,
        val amount: Balance<Currency>,
        val creationDate: Date,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    fun copyMinus(amount: Balance<Currency>) = this.copy(amount = this.amount.minus(amount))
    fun copyPlus(amount: Balance<Currency>) = this.copy(amount = this.amount.plus(amount))
}

@CordaSerializable
enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
}
