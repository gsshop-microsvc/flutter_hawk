import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class FlutterHawk {
  static const MethodChannel _channel = const MethodChannel('flutter_hawk');
// Create storage
  static final FlutterSecureStorage storage = FlutterSecureStorage(
      aOptions: AndroidOptions(encryptedSharedPreferences: true));

  static Future<String> get(String key) async {
    return _get(key);
    // final String result = await _channel.invokeMethod('get', <String, dynamic>{
    //   'key': key,
    // });
    // return result;
  }

  static Future<String> _get(String key) async {
    final result = await storage.read(key: key);
    return result ?? '';
  }

  static Future<bool> put(String key, String value) async {
    return _put(key, value);
    // final bool result = await _channel
    //     .invokeMethod('put', <String, dynamic>{'key': key, 'value': value});
    // return result;
  }

  static Future<bool> _put(String key, String value) async {
    try {
      await storage.write(key: key, value: value);
      return true;
    } catch (e) {
      return false;
    }
  }

  static Future<bool> delete(String key) async {
    return _delete(key);
    // final bool result = await _channel.invokeMethod('delete', <String, dynamic>{
    //   'key': key,
    // });
    // return result;
  }

  static Future<bool> _delete(String key) async {
    try {
      await storage.delete(key: key);
      return true;
    } catch (e) {
      return false;
    }
  }
}
