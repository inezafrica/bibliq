package com.yourname.bibliq.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TalentWallet {
    val balance: StateFlow<Int>
    fun trySpend(amount: Int): Boolean
}

class FakeTalentWallet(initial: Int = 240) : TalentWallet {
    private val _balance = MutableStateFlow(initial)
    override val balance: StateFlow<Int> = _balance.asStateFlow()

    override fun trySpend(amount: Int): Boolean {
        val current = _balance.value
        if (current < amount) return false
        _balance.value = current - amount
        return true
    }
}
