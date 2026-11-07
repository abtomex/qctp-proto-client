package dom.dima.qctp

import android.app.Application
import dom.dima.qctp.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class QctpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QctpApplication)
            modules(dataModule, repositoryModule, interactorModule, viewModelModule)
        }
    }
}