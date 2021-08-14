package com.gsshop.mobile.flutter.flutter_hawk

import androidx.annotation.NonNull

import android.content.Context

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.orhanobut.hawk.Hawk

/** FlutterHawkPlugin */
class FlutterHawkPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var mContext: Context? = null

  companion object {
    private var registrar: PluginRegistry.Registrar? = null

    private fun setRegistrar(_registrar: PluginRegistry.Registrar) {
        registrar = _registrar
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    mContext = flutterPluginBinding.applicationContext

    print("---1111---")
    Hawk.init(mContext).build()

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_hawk")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "get") {
      var value: String = ""
      var key: String? = call.argument("key")

      try {
        print("---hawk 2222---")
        if (key != null ){
          print("---hawk 3333---" + key)
        }
        value = Hawk.get(key)
        print("---hawk 444---" + value)
      } catch(e: Exception) {
        print(e.localizedMessage)
      }

      result.success(value)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
