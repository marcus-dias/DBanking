package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.AccountContract
import com.marcus.contracts.BaseContract
import com.marcus.states.AccountState
import com.marcus.states.WalletState
import com.marcus.utils.findLedgerState
import com.marcus.utils.findState
import com.marcus.utils.getContractState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.math.BigDecimal
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateAccountFlow(private val currencyCode: String) : BaseFlow<AccountState>() {

    @Suspendable
    override fun call(): AccountState {
        val walletStateAndRef = findLedgerState<WalletState>()
        val amount = Amount.fromDecimal(BigDecimal.ZERO, Currency.getInstance(currencyCode))
        val state = AccountState(
                walletStateAndRef.getContractState().linearId,
                amount,
                Date(),
                listOf(ourIdentity)
        )
        val transactionBuilder = buildTransaction(
                AccountContract.CreateAccountCommand() to listOf(ourIdentity.owningKey)
        )
        transactionBuilder.addOutputState(state, AccountContract.CONTRACT_ID)
        val finalisedTransaction = collectSignaturesAndUpdateLedger(transactionBuilder)
        return findState(finalisedTransaction)
    }
}