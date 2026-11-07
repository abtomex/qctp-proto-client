package dom.dima.qctp.di

import dom.dima.qctp.presentation.viewmodel.CameraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { CameraViewModel(get(), get()) } // Передаем оба интерактора
}