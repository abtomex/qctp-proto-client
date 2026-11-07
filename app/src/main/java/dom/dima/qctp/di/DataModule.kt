package dom.dima.qctp.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        androidContext().getSharedPreferences("qctp_preferences", Context.MODE_PRIVATE)
    }
}