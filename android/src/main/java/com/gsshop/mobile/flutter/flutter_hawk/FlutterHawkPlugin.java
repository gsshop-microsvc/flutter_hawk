package com.gsshop.mobile.flutter.flutter_hawk;

import android.content.Context;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.orhanobut.hawk.Hawk;

/**
 * Flutter Hawk plugin — secure key-value storage (Hawk) for Flutter Android.
 */
public class FlutterHawkPlugin implements FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private Context applicationContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context context, BinaryMessenger messenger) {
        applicationContext = context;
        Hawk.init(applicationContext).build();

        channel = new MethodChannel(messenger, "flutter_hawk");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        String method = call.method;
        if ("get".equals(method)) {
            String key = call.argument("key");
            try {
                String value = key != null ? Hawk.get(key) : "";
                result.success(value != null ? value : "");
            } catch (Exception e) {
                if (e.getLocalizedMessage() != null) {
                    System.out.println(e.getLocalizedMessage());
                }
                result.success("");
            }
        } else if ("delete".equals(method)) {
            String key = call.argument("key");
            try {
                if (key != null) {
                    Hawk.delete(key);
                    result.success(true);
                } else {
                    result.success(false);
                }
            } catch (Exception e) {
                if (e.getLocalizedMessage() != null) {
                    System.out.println(e.getLocalizedMessage());
                }
                result.success(false);
            }
        } else if ("put".equals(method)) {
            String key = call.argument("key");
            String value = call.argument("value");
            try {
                if (key != null) {
                    Hawk.put(key, value);
                    result.success(true);
                } else {
                    result.success(false);
                }
            } catch (Exception e) {
                if (e.getLocalizedMessage() != null) {
                    System.out.println(e.getLocalizedMessage());
                }
                result.success(false);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
    }
}
