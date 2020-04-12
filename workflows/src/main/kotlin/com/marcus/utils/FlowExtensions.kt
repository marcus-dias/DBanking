package com.marcus.utils

import co.paralleluniverse.fibers.Suspendable
import com.marcus.states.AccountState
import com.marcus.states.WalletState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import java.util.*

fun FlowLogic<*>.getNotaryNode() = serviceHub.networkMapCache.notaryIdentities.first()

@Suspendable
inline fun <reified S : ContractState> FlowLogic<*>.findStates(finalisedTransaction: SignedTransaction) =
        finalisedTransaction.tx.outRefsOfType<S>().map { it.state.data }

@Suspendable
inline fun <reified S : ContractState> FlowLogic<*>.findState(finalisedTransaction: SignedTransaction) =
        findStates<S>(finalisedTransaction).single()

@Suspendable
inline fun <reified S : ContractState> FlowLogic<*>.findLedgerStates(): List<StateAndRef<S>> {
    val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
    return serviceHub.vaultService.queryBy<S>(criteria).states
}

@Suspendable
inline fun <reified S : ContractState> FlowLogic<*>.findLedgerState(): StateAndRef<S> {
    return findLedgerStates<S>().single()
}

@Suspendable
inline fun FlowLogic<*>.findWalletForOwner(owner: Party): StateAndRef<WalletState> {
    return findLedgerStates<WalletState>().single { it.getContractState().owner == owner }
}

@Suspendable
inline fun FlowLogic<*>.findMyWallet(): StateAndRef<WalletState> {
    return findLedgerStates<WalletState>().single { it.getContractState().owner == ourIdentity }
}

@Suspendable
inline fun FlowLogic<*>.findAccountForCurrency(currency: Currency): StateAndRef<AccountState> {
    return findLedgerStates<AccountState>().single { it.getContractState().amount.token == currency }
}

@Suspendable
inline fun <reified S : ContractState> StateAndRef<S>.getContractState(): S = state.data