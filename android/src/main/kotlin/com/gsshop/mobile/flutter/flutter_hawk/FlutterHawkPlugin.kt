package com.gsshop.mobile.flutter.flutter_hawk

import androidx.annotation.NonNull

import android.content.Context

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry.Registrar
import com.orhanobut.hawk.Hawk

/** FlutterHawkPlugin */
class FlutterHawkPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var mContext: Context? = null
  private var registrar: Registrar? = null

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val instance = FlutterHawkPlugin()
      instance.registrar = registrar
      instance.onAttachedToEngine(registrar.context(), registrar.messenger())
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    onAttachedToEngine(flutterPluginBinding.getApplicationContext(), flutterPluginBinding.getBinaryMessenger());
  }

  private fun onAttachedToEngine(applicationContext: Context, binaryMessenger: BinaryMessenger) {
    mContext = applicationContext
    Hawk.init(mContext).build()

    channel = MethodChannel(binaryMessenger, "flutter_hawk")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "get") {
      var value: String = ""
      var key: String? = call.argument("key")

      try {
        if (key != null ){
          value = Hawk.get(key)
        } else {

        }
      } catch(e: Exception) {
        print(e.localizedMessage)
      }
      result.success(value)
    } else if (call.method == "delete") {
      var key: String? = call.argument("key")

      try {
        if (key != null ){
          Hawk.delete(key)
        } else {
          result.success(false)
        }
      } catch(e: Exception) {
        print(e.localizedMessage)
        result.success(false)
      }
      result.success(true)
    } else if (call.method == "put") {
      var key: String? = call.argument("key")
      var value: String? = call.argument("value")

      try {
        if (key != null ){
          Hawk.put(key, value)
        } else {
          result.success(false)
        }
      } catch(e: Exception) {
        print(e.localizedMessage)
        result.success(false)
      }
      result.success(true)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
