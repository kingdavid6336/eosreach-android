/*
Copyright (C) 2018-present memtrip

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.memtrip.eosreach.api.actions.model

import android.os.Parcelable
import com.memtrip.eos.http.rpc.model.history.response.HistoricAccountAction
import com.memtrip.eosreach.R
import com.memtrip.eosreach.api.balance.Balance
import com.memtrip.eosreach.api.balance.ContractAccountBalance
import com.memtrip.eosreach.app.price.BalanceFormatter
import com.memtrip.eosreach.app.price.CurrencyPairFormatter
import com.memtrip.eosreach.utils.fullDateTime
import com.memtrip.eosreach.utils.toLocalDateTime
import kotlinx.android.parcel.Parcelize

sealed class AccountAction(
    val next: Long
) {

    @Parcelize
    data class Transfer(
        val tranactionId: String,
        val accountActionSequence: Long,
        val from: String,
        val to: String,
        val memo: String,
        val formattedDate: String,
        val quantityString: String,
        val quantity: Balance,
        val currencyPairValue: String,
        val transferIncoming: Boolean,
        val transferIncomingIcon: Int,
        val transferInteractingAccountName: String,
        val contractAccountBalance: ContractAccountBalance
    ) : AccountAction(accountActionSequence), Parcelable

    companion object {

        fun createTransfer(
            action: HistoricAccountAction,
            contractAccountBalance: ContractAccountBalance
        ): AccountAction.Transfer {

            @Suppress("UNCHECKED_CAST")
            val data = action.action_trace.act.data as Map<String, Any>

            val from = data["from"].toString()
            val to = data["to"].toString()
            val memo = data["memo"].toString()
            val formattedDate = action.block_time.toLocalDateTime().fullDateTime()
            val quantity = data["quantity"].toString()
            val quantityBalance: Balance = BalanceFormatter.deserialize(quantity)
            val transferIncoming = (contractAccountBalance.accountName == to)
            val transferIncomingIcon = if (transferIncoming)
                R.drawable.account_actions_list_item_in_ic else R.drawable.account_actions_list_item_out_ic
            val transferInteractingAccountName: String = if (transferIncoming) from else to

            return AccountAction.Transfer(
                action.action_trace.trx_id,
                action.account_action_seq,
                from,
                to,
                memo,
                formattedDate,
                quantity,
                quantityBalance,
                if (!contractAccountBalance.exchangeRate.unavailable) {
                    CurrencyPairFormatter.formatAmountCurrencyPairValue(
                        quantityBalance.amount,
                        contractAccountBalance.exchangeRate)
                } else { "-" },
                transferIncoming,
                transferIncomingIcon,
                transferInteractingAccountName,
                contractAccountBalance)
        }
    }
}