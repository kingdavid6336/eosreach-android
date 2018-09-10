package com.memtrip.eosreach.app.account

import android.app.Application
import com.memtrip.eosreach.R
import com.memtrip.mxandroid.MxViewModel
import io.reactivex.Observable
import javax.inject.Inject

class AccountViewModel @Inject internal constructor(
    private val accountUseCase: AccountUseCase,
    application: Application
) : MxViewModel<AccountIntent, AccountRenderAction, AccountViewState>(
    AccountViewState(view = AccountViewState.View.Idle),
    application
) {

    override fun dispatcher(intent: AccountIntent): Observable<AccountRenderAction> = when (intent) {
        is AccountIntent.Init -> getAccount(intent.accountBundle.accountName)
        is AccountIntent.Retry -> getAccount(intent.accountBundle.accountName)
        is AccountIntent.Refresh -> getAccount(intent.accountBundle.accountName, intent.page)
        AccountIntent.Idle -> Observable.just(AccountRenderAction.Idle)
        AccountIntent.NavigateToAccountList -> Observable.just(AccountRenderAction.NavigateToAccountList)
        AccountIntent.NavigateToImportKey -> Observable.just(AccountRenderAction.NavigateToImportKey)
        AccountIntent.NavigateToCreateAccount -> Observable.just(AccountRenderAction.NavigateToCreateAccount)
        AccountIntent.NavigateToSettings -> Observable.just(AccountRenderAction.NavigateToSettings)
    }

    override fun reducer(previousState: AccountViewState, renderAction: AccountRenderAction): AccountViewState = when (renderAction) {
        AccountRenderAction.Idle -> previousState.copy(view = AccountViewState.View.Idle)
        is AccountRenderAction.OnProgress -> previousState.copy(
            accountName = renderAction.accountName,
            view = AccountViewState.View.OnProgress
        )
        is AccountRenderAction.OnSuccess -> previousState.copy(
            view = AccountViewState.View.OnSuccess(renderAction.page),
            accountView = renderAction.accountView
        )
        AccountRenderAction.OnErrorFetchingAccount -> previousState.copy(view = AccountViewState.View.OnErrorFetchingAccount)
        AccountRenderAction.OnErrorFetchingBalances -> previousState.copy(view = AccountViewState.View.OnErrorFetchingAccount)
        AccountRenderAction.NavigateToAccountList -> previousState.copy(view = AccountViewState.View.NavigateToAccountList)
        AccountRenderAction.NavigateToImportKey -> previousState.copy(view = AccountViewState.View.NavigateToImportKey)
        AccountRenderAction.NavigateToCreateAccount -> previousState.copy(view = AccountViewState.View.NavigateToCreateAccount)
        AccountRenderAction.NavigateToSettings -> previousState.copy(view = AccountViewState.View.NavigateToSettings)
    }

    override fun filterIntents(intents: Observable<AccountIntent>): Observable<AccountIntent> = Observable.merge(
        intents.ofType(AccountIntent.Init::class.java).take(1),
        intents.filter {
            !AccountIntent.Init::class.java.isInstance(it)
        }
    )

    private fun getAccount(accountName: String, page: AccountPagerFragment.Page = AccountPagerFragment.Page.BALANCE): Observable<AccountRenderAction> {
        return accountUseCase.getAccountDetails(
            "eosio.token",
            accountName,
            context().getString(R.string.app_default_eos_currency)
        ).map {
            if (it.success) {
                AccountRenderAction.OnSuccess(it, page)
            } else {
                onError(it.error!!)
            }
        }.toObservable().startWith(AccountRenderAction.OnProgress(accountName))
    }

    private fun onError(error: AccountView.Error): AccountRenderAction = when (error) {
        AccountView.Error.FetchingAccount -> AccountRenderAction.OnErrorFetchingAccount
        AccountView.Error.FetchingBalances -> AccountRenderAction.OnErrorFetchingBalances
    }
}