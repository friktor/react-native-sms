package com.friktor.reactnativesms

import android.content.BroadcastReceiver
import android.telephony.SmsManager
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.util.Log

// React Native need context binders
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise

// React Native data transporter
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments

fun codeDeliverMessage(code: Int) = when (code) {
  Activity.RESULT_CANCELED -> "RESULT_CANCELED"
  Activity.RESULT_OK -> "RESULT_OK"
  else -> "UNKNOWN_DELIVER_CODE"
}

class BroadcastDeliver (private val reactContext: ReactApplicationContext): BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    val deliverResultCode = codeDeliverMessage(getResultCode())
    val chunkIndex = intent?.getStringExtra("chunkIndex")
    val groupId = intent?.getStringExtra("groupId")
    
    Log.d("RNSmsManager", "result of sms deliver: $deliverResultCode")

    val result = Arguments.createMap()
    result.putString("status", deliverResultCode)

    if (chunkIndex != null) result.putString("chunkIndex", chunkIndex)
    if (groupId != null) result.putString("groupId", groupId)

    if (!reactContext.hasActiveCatalystInstance()) { return }
    
    try {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(EVENT_NAME, result)
    } catch (error: Exception) {
      Log.d("RNSmsManager", "error of deliver sms", error)  
    }
  }

  companion object {
    private val EVENT_NAME = "com.friktor.reactnativesms:sms_deliver_result"
  }
}