package com.memtrip.eosreach.api.transfer

import android.os.Parcelable
import com.memtrip.eosreach.api.balance.ContractAccountBalance
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ActionReceipt(
    val transactionId: String,
    val authorizingAccountName: String
) : Parcelable