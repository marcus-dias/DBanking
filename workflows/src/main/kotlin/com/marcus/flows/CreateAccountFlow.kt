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
import java.math.BigDecimal
import java.util.*


class CreateAccountFlow(private val currencyCode: String) : BaseFlow<AccountState>() {

    @Suspendable
    override fun call(): AccountState {
        val walletStateAndRef = findLedgerState<WalletState>()

        val amount = Amount.fromDecimal(BigDecimal.ZERO, Currency.getInstance(currencyCode))
        val state = AccountState(walletStateAndRef.getContractState().linearId, amount, Date())
        val transactionBuilder = buildTransaction()

        transactionBuilder.addOutputState(state, AccountContract.CONTRACT_ID)
        val signInitialTransaction = signTransaction(transactionBuilder)

        signInitialTransaction.verifyRequiredSignatures()

        val finalisedTransaction = updateLedger(signInitialTransaction)
        return findState(finalisedTransaction)
    }

    override fun createCommands(): List<Command<out BaseContract.MyCommand>> {
        return listOf(
                Command(AccountContract.CreateAccountCommand(), listOf(ourIdentity.owningKey))
        )
    }

}