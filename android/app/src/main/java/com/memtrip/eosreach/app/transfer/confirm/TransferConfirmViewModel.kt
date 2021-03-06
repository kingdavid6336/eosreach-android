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
package com.memtrip.eosreach.app.transfer.confirm

import android.app.Application
import com.memtrip.eosreach.R
import com.memtrip.eosreach.api.transfer.ActionReceipt
import com.memtrip.eosreach.db.transaction.InsertTransactionLog
import com.memtrip.eosreach.db.transaction.TransactionLogEntity
import com.memtrip.eosreach.utils.fullDateTime
import com.memtrip.eosreach.utils.toLocalDateTime

import com.memtrip.mxandroid.MxViewModel
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject

class TransferConfirmViewModel @Inject internal constructor(
    private val transferUseCase: TransferUseCase,
    private val insertTransactionLog: InsertTransactionLog,
    application: Application
) : MxViewModel<TransferConfirmIntent, TransferConfirmRenderAction, TransferConfirmViewState>(
    TransferConfirmViewState(view = TransferConfirmViewState.View.Idle),
    application
) {

    override fun dispatcher(intent: TransferConfirmIntent): Observable<TransferConfirmRenderAction> = when (intent) {
        TransferConfirmIntent.Idle -> Observable.just(TransferConfirmRenderAction.Idle)
        is TransferConfirmIntent.Init -> Observable.just(TransferConfirmRenderAction.Populate(intent.transferFormData))
        is TransferConfirmIntent.Transfer -> transfer(intent.transferRequestData)
        is TransferConfirmIntent.ViewLog -> Observable.just(TransferConfirmRenderAction.ViewLog(intent.log))
    }

    override fun reducer(previousState: TransferConfirmViewState, renderAction: TransferConfirmRenderAction): TransferConfirmViewState = when (renderAction) {
        TransferConfirmRenderAction.Idle -> previousState.copy(
            view = TransferConfirmViewState.View.Idle)
        is TransferConfirmRenderAction.Populate -> previousState.copy(
            view = TransferConfirmViewState.View.Populate(renderAction.transferFormData))
        TransferConfirmRenderAction.OnProgress -> previousState.copy(
            view = TransferConfirmViewState.View.OnProgress)
        is TransferConfirmRenderAction.OnError -> previousState.copy(
            view = TransferConfirmViewState.View.OnError(renderAction.message, renderAction.log))
        is TransferConfirmRenderAction.OnSuccess -> previousState.copy(
            view = TransferConfirmViewState.View.OnSuccess(renderAction.transferReceipt))
        is TransferConfirmRenderAction.ViewLog -> previousState.copy(
            view = TransferConfirmViewState.View.ViewLog(renderAction.log))
    }

    override fun filterIntents(intents: Observable<TransferConfirmIntent>): Observable<TransferConfirmIntent> = Observable.merge(
        intents.ofType(TransferConfirmIntent.Init::class.java).take(1),
        intents.filter {
            !TransferConfirmIntent.Init::class.java.isInstance(it)
        }
    )

    private fun transfer(transferRequestData: TransferRequestData): Observable<TransferConfirmRenderAction> {
        return transferUseCase.transfer(
            transferRequestData.contractAccountBalance.contractName,
            transferRequestData.fromAccount,
            transferRequestData.toAccount,
            transferRequestData.quantity,
            transferRequestData.memo
        ).flatMap { result ->
            if (result.success) {
                val transaction = result.data!!
                insertTransactionLog.insert(TransactionLogEntity(
                    transactionId = transaction.transaction_id,
                    formattedDate = Date().toLocalDateTime().fullDateTime())
                ).toSingleDefault<TransferConfirmRenderAction>(
                    TransferConfirmRenderAction.OnSuccess(
                        ActionReceipt(
                            transaction.transaction_id,
                            transferRequestData.fromAccount)
                    )
                )
            } else {
                Single.just(TransferConfirmRenderAction.OnError(
                    context().getString(R.string.app_dialog_transaction_error_body),
                    result.apiError!!.body))
            }
        }.toObservable().startWith(TransferConfirmRenderAction.OnProgress)
    }
}