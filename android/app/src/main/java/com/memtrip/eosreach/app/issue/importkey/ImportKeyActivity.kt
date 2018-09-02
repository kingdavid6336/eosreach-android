package com.memtrip.eosreach.app.issue.importkey

import android.os.Bundle
import com.jakewharton.rxbinding2.view.RxView
import com.memtrip.eosreach.R
import com.memtrip.eosreach.app.MviActivity
import com.memtrip.eosreach.app.ViewModelFactory
import io.reactivex.Observable
import kotlinx.android.synthetic.main.issue_import_key_activity.*
import javax.inject.Inject

abstract class ImportKeyActivity
    : MviActivity<ImportKeyIntent, ImportKeyRenderAction, ImportKeyViewState, ImportKeyViewLayout>(), ImportKeyViewLayout {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var render: ImportKeyViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.issue_import_key_activity)
    }

    override fun intents(): Observable<ImportKeyIntent> {
        return RxView.clicks(issue_import_key_import_button).map {
            ImportKeyIntent.ImportKey(issue_import_key_private_key_value_input.text.toString())
        }
    }

    override fun layout(): ImportKeyViewLayout = this

    override fun model(): ImportKeyViewModel = getViewModel(viewModelFactory)

    override fun render(): ImportKeyViewRenderer = render

    override fun showProgress() {
    }

    override fun showError(error: String) {
        issue_import_key_private_key_value_label.error = error
    }

    abstract override fun inject()

    abstract override fun success()
}