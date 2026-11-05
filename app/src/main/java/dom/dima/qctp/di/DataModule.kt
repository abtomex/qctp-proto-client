package dom.dima.qctp.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

//    single<AuthApiService> {
//        Retrofit.Builder()
//            .baseUrl(BuildConfig.AUTH_BASE_URL)
//            .client(get())
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(AuthApiService::class.java)
//    }

// SharedPreferences
    single {
        androidContext()
            .getSharedPreferences("qctp_preferences", Context.MODE_PRIVATE)
    }

}
