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

fun codeSentMessage(code: Int) = when (code) {
  SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "RESULT_ERROR_GENERIC_FAILURE"
  SmsManager.RESULT_ERROR_NO_SERVICE -> "RESULT_ERROR_NO_SERVICE"
  SmsManager.RESULT_ERROR_RADIO_OFF -> "RESULT_ERROR_RADIO_OFF"
  SmsManager.RESULT_ERROR_NULL_PDU -> "RESULT_ERROR_NULL_PDU"

  Activity.RESULT_OK -> "RESULT_OK"
  else -> "UNKNOWN_SENT_CODE"
}

class BroadcastSent (private val reactContext: ReactApplicationContext): BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    val sentResultCode = codeSentMessage(getResultCode())
    val chunkIndex = intent?.getStringExtra("chunkIndex")
    val groupId = intent?.getStringExtra("groupId")

    Log.d("RNSmsManager", "message sent: $sentResultCode")
    
    val result = Arguments.createMap()
    result.putString("status", sentResultCode)

    if (chunkIndex != null) result.putString("chunkIndex", chunkIndex)
    if (groupId != null) result.putString("groupId", groupId)

    if (!reactContext.hasActiveCatalystInstance()) { return }

    try {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(EVENT_NAME, result)
    } catch (error: Exception) {
      Log.d("RNSmsManager", "error of sent sms", error)  
    }
  }

  companion object {
    private val EVENT_NAME = "com.friktor.reactnativesms:sms_sent_result"
  }
}