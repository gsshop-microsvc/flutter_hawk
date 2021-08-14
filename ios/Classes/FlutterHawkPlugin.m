#import "FlutterHawkPlugin.h"
#if __has_include(<flutter_hawk/flutter_hawk-Swift.h>)
#import <flutter_hawk/flutter_hawk-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_hawk-Swift.h"
#endif

@implementation FlutterHawkPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterHawkPlugin registerWithRegistrar:registrar];
}
@end
