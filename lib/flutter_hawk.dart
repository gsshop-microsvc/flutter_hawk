import 'dart:async';

import 'package:flutter/services.dart';

class FlutterHawk {
  static const MethodChannel _channel = const MethodChannel('flutter_hawk');

  static Future<String> get(String key) async {
    final String result = await _channel.invokeMethod('get', <String, dynamic>{
      'key': key,
    });
    return result;
  }

  static Future<bool> put(String key, String value) async {
    final bool result = await _channel
        .invokeMethod('put', <String, dynamic>{'key': key, 'value': value});
    return result;
  }

  static Future<bool> delete(String key) async {
    final bool result = await _channel.invokeMethod('delete', <String, dynamic>{
      'key': key,
    });
    return result;
  }
}
