package io.github.zdyoyoyo.backendquiz

import android.app.Application

/** App 啟動時最先建立的物件（在 AndroidManifest 的 android:name 指定），整個 App 只有一個。 */
class QuizApp : Application() {
    lateinit var container: AppContainer // lateinit：保證使用前會初始化，不用宣告成可為 null
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
