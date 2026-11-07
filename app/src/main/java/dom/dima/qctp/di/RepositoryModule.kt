package dom.dima.qctp.di

import dom.dima.qctp.data.FileRepositoryImpl
import dom.dima.qctp.domain.FileRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<FileRepository> {
        FileRepositoryImpl(get())
    }

}

