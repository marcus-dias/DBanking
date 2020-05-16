package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.Balance
import com.marcus.contracts.AccountContract
import com.marcus.states.AccountState
import com.marcus.states.AccountStatus
import com.marcus.states.WalletState
import com.marcus.utils.findAccountForCurrencyOrNull
import com.marcus.utils.findLedgerState
import com.marcus.utils.findState
import com.marcus.utils.getContractState
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.math.BigDecimal
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateAccountFlow(private val currency: Currency) : BaseFlow<AccountState>() {

    @Suspendable
    override fun call(): AccountState {
        val walletStateAndRef = findLedgerState<WalletState>()
        require(findAccountForCurrencyOrNull(currency) == null){
            "Not allowed to have multiple account for the same currency."
        }
        val amount = Balance.fromDecimal(BigDecimal.ZERO, currency)
        val state = AccountState(
                walletStateAndRef.getContractState().linearId,
                amount,
                Date(),
                AccountStatus.ACTIVE,
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