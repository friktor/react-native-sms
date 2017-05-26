package com.friktor.reactnativesms

// Exceptions kotlin
import kotlin.sequences.generateSequence
import kotlin.RuntimeException

// Android
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import android.util.Log
import android.database.Cursor

// React Native need context binders
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

// React Native data transporter
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

// KOI Libs
// import com.mcxiaoke.koi.ext.*

class SmsModule
(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "SmsAndroid"

  private val appDefaultStatus: Boolean
    get() {
      val currentActivity = getCurrentActivity() ?: return false
      val packageName = currentActivity.getPackageName()
      val isDefault: Boolean = Sms.getDefaultSmsPackage(currentActivity)?.equals(packageName) ?: false
      return isDefault
    }

  @ReactMethod
  fun isDefaultSmsApp(promise: Promise) {
    val isDefault = appDefaultStatus
    promise.resolve(isDefault)
  }

  @ReactMethod
  fun setAsDefaultApp(promise: Promise) {
    val currentActivity: Activity? = getCurrentActivity()

    if (currentActivity != null) {
      val packageName = currentActivity.getPackageName()

      try {
        val intent: Intent? = Intent(Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent?.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)

        currentActivity.startActivity(intent)
        promise.resolve(true)
      } catch (error: Exception) {
        promise.reject(SET_AS_DEFAULT_ERROR, error)
      }
    } else {
      val error = RuntimeException("activity is no allowed")
      promise.reject(SET_AS_DEFAULT_ERROR, error)
    }
  }

  @ReactMethod
  fun send(phoneNumberString: String, body: String?, sendType: String, promise: Promise) {
    // send directly if user requests and android greater than 4.4
    if (sendType.equals("sendDirect") && body != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      try {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumberString, null, body, null, null)
        promise.resolve(true)
      } catch (error: Exception) {
        promise.reject(SMS_SEND_ERROR, error)
      }

    } else {
      val sendIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumberString.trim()))
      sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      if (body != null) {
        sendIntent.putExtra("sms_body", body)
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val defaultSmsPackageName = Sms.getDefaultSmsPackage(getCurrentActivity())
        if (defaultSmsPackageName != null) {
          sendIntent.setPackage(defaultSmsPackageName)
        }
      }

      try {
        this.reactContext.startActivity(sendIntent)
        promise.resolve(true)
      } catch (error: Exception) {
        promise.reject(SMS_SEND_ERROR, error)
      }

    }
  }

  @ReactMethod
  fun list(filter: ReadableMap, promise: Promise) {
    val currentActivity: Activity? = getCurrentActivity()

    // @FILTERS
    val uri_filter: String = if (filter.hasKey("box")) filter.getString("box") else "inbox"
    val fread: Int = if (filter.hasKey("read")) filter.getInt("read") else -1
    val fid: Int = if (filter.hasKey("_id")) filter.getInt("_id") else -1

    val faddress: String = if (filter.hasKey("address")) filter.getString("address") else ""
    val fcontent: String = if (filter.hasKey("body")) filter.getString("body") else ""

    val indexFrom: Int = if (filter.hasKey("indexFrom")) filter.getInt("indexFrom") else 0
    val maxCount: Int = if (filter.hasKey("maxCount")) filter.getInt("maxCount") else -1

    val cursor: Cursor? = currentActivity?.getContentResolver()?.query(Uri.parse("content://sms/" + uri_filter), null, "", null, null)
    val smsList = Arguments.createArray()
    var counter: Int = 0

    fun _iteratorSafe_ (): Boolean {
      val isAllowNext = cursor?.moveToNext()
      return (isAllowNext != null && isAllowNext)
    }

    do {
      var isMatched: Boolean
      val _address = cursor?.getString(cursor.getColumnIndex("address"))
      val _fcontent = cursor?.getString(cursor.getColumnIndex("body"))

      if (fid > -1)
        isMatched = fid == cursor?.getInt(cursor.getColumnIndex("_id"))
      else if (fread > -1)
        isMatched = fread == cursor?.getInt(cursor.getColumnIndex("read"))
      else if (faddress.length > 0)
        isMatched = faddress.equals(_address)
      else if (fcontent.length > 0)
        isMatched = fcontent.equals(_fcontent)
      else {
        isMatched = true
      }

      if (isMatched && counter >= indexFrom) {
        if (maxCount > 0 && counter >= indexFrom + maxCount) break

        val sms = getSmsMapFromCursor(cursor)
        smsList?.pushMap(sms)
      }
    } while (_iteratorSafe_())

    cursor?.close()

    // val result = Arguments.createMap()
    // result.putInt("length", counter)
    // result.putArray("list", smsList)

    promise.resolve(smsList)
  }

  private fun getSmsMapFromCursor(cursorSms: Cursor?): WritableMap {
    val sms = Arguments.createMap()

    val columnNumber = cursorSms?.getColumnCount()
    val keys = cursorSms?.getColumnNames()

    if (columnNumber != null && keys != null) for (columnIndex in 0..columnNumber - 1) {
      val key = keys[columnIndex]

      when (cursorSms.getType(columnIndex)) {
        0 -> sms.putNull(key)
        1 -> sms.putInt(key, cursorSms.getInt(columnIndex))
        2 -> sms.putDouble(key, cursorSms.getFloat(columnIndex).toDouble())
        3 -> sms.putString(key, cursorSms.getString(columnIndex))
        4 -> {}
      }
    }

    return sms
  }

  companion object {
    private val TAG = SmsModule::class.java.getSimpleName()

    // Error codes
    private val GET_STATUS_DEFAULT_ERROR = "GET_STATUS_DEFAULT_ERROR"
    private val SET_AS_DEFAULT_ERROR = "SET_AS_DEFAULT_ERROR"
    private val SMS_SEND_ERROR = "SMS_SEND_ERROR"
    private val SMS_LIST_ERROR = "SMS_LIST_ERROR"
  }
}