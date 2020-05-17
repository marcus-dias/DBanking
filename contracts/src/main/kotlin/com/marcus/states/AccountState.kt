package com.marcus.states

import com.marcus.Balance
import com.marcus.contracts.AccountContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * Account State used to store the balance that a certain wallet has for the given currency
 */
@BelongsToContract(AccountContract::class)
data class AccountState(
        val walletStateId: UniqueIdentifier,
        val balance: Balance<Currency>,
        val creationDate: Date,
        val state: AccountStatus,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    fun copyMinus(amount: Balance<Currency>) = this.copy(balance = this.balance.minus(amount))
    fun copyPlus(amount: Balance<Currency>) = this.copy(balance = this.balance.plus(amount))
}

/**
 * Accounts Status possible values
 */
@CordaSerializable
enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
}