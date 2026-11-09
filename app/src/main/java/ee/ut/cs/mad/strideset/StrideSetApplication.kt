package ee.ut.cs.mad.strideset

import android.app.Application
import ee.ut.cs.mad.strideset.data.AppContainer
import ee.ut.cs.mad.strideset.data.DefaultAppContainer

class StrideSetApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}