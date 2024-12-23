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
    when (call.method) {
      "get" -> {
        val key = call.argument<String>("key")
        try {
          val value = key?.let { Hawk.get<String>(it) } ?: ""
          result.success(value)
        } catch(e: Exception) {
          e.localizedMessage?.let { println(it) }
          result.success("")
        }
      }
      "delete" -> {
        val key = call.argument<String>("key")
        try {
          if (key != null) {
            Hawk.delete(key)
            result.success(true)
          } else {
            result.success(false)
          }
        } catch(e: Exception) {
          e.localizedMessage?.let { println(it) }
          result.success(false)
        }
      }
      "put" -> {
        val key = call.argument<String>("key")
        val value = call.argument<String>("value")
        try {
          if (key != null) {
            Hawk.put(key, value)
            result.success(true)
          } else {
            result.success(false)
          }
        } catch(e: Exception) {
          e.localizedMessage?.let { println(it) }
          result.success(false)
        }
      }
      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
