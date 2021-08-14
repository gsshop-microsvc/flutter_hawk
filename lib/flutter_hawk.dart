import 'dart:async';

import 'package:flutter/services.dart';

class FlutterHawk {
  static const MethodChannel _channel = const MethodChannel('flutter_hawk');

  static Future<String?> get(String key) async {
    final String? value = await _channel.invokeMethod('get', <String, dynamic>{
      'key': key,
    });
    return value;
  }
}
