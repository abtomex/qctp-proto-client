package dom.dima.qctp.di

import dom.dima.qctp.domain.FileSendInteractor
import dom.dima.qctp.domain.QrCameraInteractor
import dom.dima.qctp.domain.impl.FileSendInteractorImpl
import dom.dima.qctp.domain.impl.QrCameraInteractorImpl
import org.koin.dsl.module

val interactorModule = module {
    single<QrCameraInteractor> { QrCameraInteractorImpl(get()) }
    single<FileSendInteractor> { FileSendInteractorImpl(get()) } // Теперь нужен Context
}
