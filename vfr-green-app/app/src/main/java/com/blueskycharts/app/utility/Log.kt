package com.blueskycharts.app.utility

class Log {
    companion object {
        private val shouldLog: Boolean = com.blueskycharts.app.BuildConfig.DEBUG

        fun i(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.i(tag, msg)
            }
        }

        fun e(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.e(tag, msg)
            }
        }

        fun d(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.d(tag, msg)
            }
        }

        fun v(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.v(tag, msg)
            }
        }

        fun w(tag: String?, msg: String) {
            if (shouldLog) {
                android.util.Log.w(tag, msg)
            }
        }
    }
}