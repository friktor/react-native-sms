package com.friktor.reactnativesms

// Exceptions kotlin
import kotlin.sequences.generateSequence
import kotlin.collections.MutableList
import kotlin.RuntimeException

// Android
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.content.ContentResolver
import android.provider.Telephony.Sms
import android.content.IntentFilter
import android.telephony.SmsManager
import android.app.PendingIntent
import android.database.Cursor
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.net.Uri

// React Native need context binders
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.LifecycleEventListener
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

// Libtary for other actions
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.*
import org.jetbrains.anko.coroutines.experimental.bg

class SmsModule
(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  
  // Lifecycle handler for stop or start wathing get pending results
  private val lifecycleHandlers = object: LifecycleEventListener {
    public override fun onHostResume() {
      val context = reactContext.getBaseContext()
      context.registerReceiver(deliverReciever, IntentFilter(SMS_DELIVERED))
      context.registerReceiver(sentReciever, IntentFilter(SMS_SENT))
    }

    public override fun onHostPause() {
      val context = reactContext.getBaseContext()
      context.unregisterReceiver(deliverReciever)
      context.unregisterReceiver(sentReciever)
    }

    public override fun onHostDestroy() {
      val context = reactContext.getBaseContext()
      context.unregisterReceiver(deliverReciever)
      context.unregisterReceiver(sentReciever)
    }
  }

  // Initialize lifecycle watcher
  init {
    reactContext.addLifecycleEventListener(lifecycleHandlers)
  }

  // Recievers
  private val deliverReciever = BroadcastDeliver(getReactApplicationContext())
  private val sentReciever = BroadcastSent(getReactApplicationContext())

  override fun getName(): String = "SmsAndroid"

  // private function for get status of current app
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

  // Method for set current app as default sms manager app
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
        promise.reject(SMS_ERROR_SET_DEFAULT, error)
      }
    } else {
      val error = RuntimeException("activity is no allowed")
      promise.reject(SMS_ERROR_SET_DEFAULT, error)
    }
  }

  @ReactMethod
  fun send(phone: String, text: String, uniqueId: String, promise: Promise) {
    try {
      val context = reactContext.getBaseContext()
      val manager = SmsManager.getDefault()

      // Slice message by parts by size
      val multipartSmsText: ArrayList<String> = manager.divideMessage(text)
      var smsChunksLength = multipartSmsText.size

      // Handlers Intents & Resulted message chunks
      val deliveredHandlers: ArrayList<PendingIntent?> = ArrayList(smsChunksLength)
      val sentHandlers: ArrayList<PendingIntent?> = ArrayList(smsChunksLength)
      val resultChunks = Arguments.createArray()

      for ((index, chunk) in multipartSmsText.withIndex()) {
        // Delivered intent with extra config
        val deliveredIntent = Intent(SMS_DELIVERED)
        deliveredIntent.putExtra("groupId", uniqueId)
        deliveredIntent.putExtra("chunkIndex", index)
        val deliveredWaiting = PendingIntent.getBroadcast(context, 0, deliveredIntent, 0)
        deliveredHandlers.add(deliveredWaiting)
        
        // Sent intent with extra config
        val sentIntent = Intent(SMS_SENT)
        deliveredIntent.putExtra("groupId", uniqueId)
        deliveredIntent.putExtra("chunkIndex", index)
        val sentWaiting = PendingIntent.getBroadcast(context, 0, sentIntent, 0)
        sentHandlers.add(sentWaiting)

        // Push result chunk for sent
        resultChunks.pushString(chunk)
      }

      // Send sms message
      manager.sendMultipartTextMessage(
        phone, null, multipartSmsText,
        // Recievers
        sentHandlers,
        deliveredHandlers
      )

      // Make basic sent result
      val result = Arguments.createMap()
      result.putArray("messageGroup", resultChunks)
      result.putString("groupId", uniqueId)

      promise.resolve(result)
    } catch (error: Exception) {
      promise.reject(SMS_ERROR_SEND, error)
    }
  }

  @ReactMethod
  fun list(box: String, paginate: ReadableMap, filter: ReadableMap, promise: Promise) {
    // Schemas
    val allowedTypes = arrayOf(
      ReadableType.String,
      ReadableType.Number
    )

    val fieldsSchema = mapOf(
      "address" to "string",
      "read" to "integer",
      "body" to "string",
      "_id" to "integer"
    )
    
    val currentActivity: Activity? = getCurrentActivity()

    // Paginate
    val enablePaginate: Boolean = if (paginate.hasKey("enable")) paginate.getBoolean("enable") else true
    val limit: Int = if (paginate.hasKey("limit")) paginate.getInt("limit") else 50
    val skip: Int = if (paginate.hasKey("skip")) paginate.getInt("skip") else 0

    // Initial query data
    var querySelection: MutableList<String> = mutableListOf()
    var argsSelection: MutableList<String> = mutableListOf()

    // Build final query string by allowed filter keys
    for ((key, schemaType) in fieldsSchema) {
      if (filter.hasKey(key)) {
        val elementType = filter.getType(key)

        if (allowedTypes.contains(elementType)) {
          querySelection.add("$key=?")
          when (schemaType) {
            "string" -> argsSelection.add(filter.getString(key))
            "integer" -> argsSelection.add(filter.getInt(key).toString())
          }
        }
      }
    }

    // Make final paginate & query properties
    val paginate: String? = if (enablePaginate) "address LIMIT $limit OFFSET $skip" else null
    val query: String = querySelection.joinToString(" and ")
    val args: Array<String> = argsSelection.toTypedArray()

    try {
      val resolver: ContentResolver? = currentActivity?.getContentResolver()
      val cursor: Cursor? = resolver?.query(Uri.parse("content://sms/$box"),
        null, query, args, paginate
      )

      val list: WritableArray = Arguments.createArray()

      generateSequence {
        // @TODO: fix this ugly non-null expression by kotlin skyle
        if (cursor != null) {
          if (cursor.moveToNext()) { cursor }
          else { null }
        } else {
          null
        }
      }.forEach {
        val sms = getSmsFromCursor(it)
        list.pushMap(sms)
      }

      cursor?.close()
      promise.resolve(list)
    } catch (error: Exception) {
      Log.d("RNSmsManager", "error in get list from cursor", error)
      promise.reject(SMS_ERROR_LIST, error)
    }
  }

  private fun getSmsFromCursor(cursorSms: Cursor?): WritableMap {
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

    // State codes
    private val SMS_DELIVERED = "SMS_DELIVERED"
    private val SMS_SENT = "SMS_SENT"

    // Error codes
    private val SMS_ERROR_GET_STATUS_D = "SMS_ERROR_GET_STATUS_D"
    private val SMS_ERROR_SET_DEFAULT = "SMS_ERROR_SET_DEFAULT"
    private val SMS_ERROR_SEND = "SMS_ERROR_SEND"
    private val SMS_ERROR_LIST = "SMS_ERROR_LIST"
  }
}