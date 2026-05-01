package pl.fitcoach

import android.app.Application
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FitCoachApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
    }
}
