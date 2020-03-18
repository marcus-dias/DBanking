package com.marcus.flows

import co.paralleluniverse.fibers.Suspendable
import com.marcus.contracts.AccountContract
import com.marcus.contracts.BaseContract
import com.marcus.states.AccountState
import com.marcus.states.WalletState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import java.math.BigDecimal
import java.util.*


class CreateAccountFlow(private val currencyCode: String) : BaseFlow<AccountState>() {

    override fun createCommands(): List<Command<out BaseContract.MyCommand>> {
        return listOf()
    }

    @Suspendable
    override fun call(): AccountState {
        val findLedgerState = findLedgerState<WalletState>()

        val amount = Amount.fromDecimal(BigDecimal.ZERO, Currency.getInstance(currencyCode))
        val state = AccountState(findLedgerState.state.data.linearId, amount, Date())
        val transactionBuilder = buildTransaction()

        transactionBuilder.addOutputState(state, AccountContract.ACCOUNT_CONTRACT_ID)
        val signInitialTransaction = signTransaction(transactionBuilder)

        signInitialTransaction.verifyRequiredSignatures()

        val finalisedTransaction = updateLedger(signInitialTransaction)
        return findState(finalisedTransaction)
    }
}