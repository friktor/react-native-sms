package com.friktor.reactnativesms

import android.app.Activity
import android.view.View
import java.util.*

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.facebook.react.uimanager.ReactShadowNode

class SmsPackage : ReactPackage {
  private var mModuleInstance: SmsModule? = null

  override fun createNativeModules(reactContext: ReactApplicationContext): List<SmsModule?> {
    mModuleInstance = SmsModule(reactContext)
    return Arrays.asList(mModuleInstance)
  }

  override fun createJSModules(): List<Class<out JavaScriptModule>> {
    return Collections.emptyList()
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<View, ReactShadowNode>> {
    val initialized: List<ViewManager<View, ReactShadowNode>> = Collections.emptyList()
    return initialized
  }
}