import 'dart:developer';
import 'package:flutter/services.dart';
import 'package:get/instance_manager.dart';
import 'package:get/state_manager.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:app_lock_flutter/executables/controllers/apps_controller.dart';
import 'package:app_lock_flutter/services/constant.dart';


import 'permission_controller.dart';

class MethodChannelController extends GetxController implements GetxService {
  static const platform = MethodChannel('flutter.native/helper');

  bool isOverlayPermissionGiven = false;
  bool isUsageStatPermissionGiven = false;
  bool isNotificationPermissionGiven = false;
  bool isDeviceAdminActive = false;
  bool isBatteryOptimizationIgnored = false;
  bool isBiometricEnabled = false;
  bool isBiometricAvailable = false;

  Future<bool> checkBiometricStatus() async {
    try {
      isBiometricAvailable = (await platform.invokeMethod('isBiometricAvailable') as bool?) ?? false;
      isBiometricEnabled = (await platform.invokeMethod('isBiometricEnabled') as bool?) ?? false;
      update();
      return isBiometricEnabled;
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.", name: "checkBiometricStatus");
      isBiometricAvailable = false;
      isBiometricEnabled = false;
      update();
      return false;
    }
  }

  Future<void> setBiometricEnabled(bool enabled) async {
    try {
      await platform.invokeMethod('setBiometricEnabled', enabled);
      isBiometricEnabled = enabled;
      update();
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.", name: "setBiometricEnabled");
    }
  }

  Future<bool> checkAdminStatus() async {
    isDeviceAdminActive = await isAdminActive();
    update();
    return isDeviceAdminActive;
  }

  Future<bool> checkBatteryStatus() async {
    isBatteryOptimizationIgnored = await isIgnoringBatteryOptimizations();
    update();
    return isBatteryOptimizationIgnored;
  }

  Future<bool> checkOverlayPermission() async {
    try {
      return await platform
          .invokeMethod('checkOverlayPermission')
          .then((value) {
        log("$value", name: "checkOverlayPermission");
        isOverlayPermissionGiven = value as bool;
        update();
        return isOverlayPermissionGiven;
      });
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
      isOverlayPermissionGiven = false;
      update();
      return isOverlayPermissionGiven;
    }
  }

  Future<bool> checkNotificationPermission() async {
    return isNotificationPermissionGiven =
        await Permission.notification.isGranted;
  }

  Future<bool> checkUsageStatePermission() async {
    isUsageStatPermissionGiven = (await platform.invokeMethod('checkUsagePermission') as bool?) ?? false;
    update();
    return isUsageStatPermissionGiven;
  }

  addToLockedAppsMethod() async {
    try {
      Map<String, dynamic> data = {
        "app_list": Get.find<AppsController>().lockList.map((e) {
          return {
            "app_name": e.application!.appName,
            "package_name": e.application!.packageName,
            "file_path": e.application!.apkFilePath,
          };
        }).toList()
      };
      await setPassword();
      await platform.invokeMethod('addToLockedApps', data).then((value) {
        log("$value", name: "addToLockedApps CALLED");
      });
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }

  Future setPassword() async {
    const storage = FlutterSecureStorage();
    try {
      String hash = await storage.read(key: AppConstants.setPassCode) ?? "";
      String salt = await storage.read(key: "passcode_salt") ?? "";
      log("Hash: $hash, Salt: $salt", name: "PASSWORD--");
      if (hash != "" && salt != "") {
        await platform.invokeMethod('setPasswordInNative', {
          "hash": hash,
          "salt": salt,
        }).then((value) {
          log("$value", name: "setPasswordInNative CALLED");
        });
      }
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }

  Future stopForeground() async {
    try {
      await platform.invokeMethod('stopForeground', "").then((value) {
        log("$value", name: "stopForeground CALLED");
      });
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }

  Future<bool> askNotificationPermission() async {
    // await AppSettings.openAppSettings();
    await Get.find<PermissionController>()
        .getPermission(Permission.notification);
    isNotificationPermissionGiven = await Permission.notification.isGranted;
    update();
    return isNotificationPermissionGiven;
  }

  Future<bool> askOverlayPermission() async {
    try {
      return await platform.invokeMethod('askOverlayPermission').then((value) {
        log("$value", name: "askOverlayPermission");
        isOverlayPermissionGiven = (value as bool);
        update();
        return isOverlayPermissionGiven;
      });
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
      return false;
    }
  }

  Future<bool> askUsageStatsPermission() async {
    try {
      return await platform
          .invokeMethod('askUsageStatsPermission')
          .then((value) {
        log("$value", name: "askUsageStatsPermission");
        return (value as bool);
      });
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
      return false;
    }
  }

  Future<bool> isIgnoringBatteryOptimizations() async {
    try {
      return await platform.invokeMethod('isIgnoringBatteryOptimizations');
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
      return false;
    }
  }

  Future requestIgnoreBatteryOptimizations() async {
    try {
      await platform.invokeMethod('requestIgnoreBatteryOptimizations');
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }

  Future<bool> isAdminActive() async {
    try {
      return await platform.invokeMethod('isAdminActive');
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
      return false;
    }
  }

  Future requestAdmin() async {
    try {
      await platform.invokeMethod('requestAdmin');
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }

  Future removeAdmin() async {
    try {
      await platform.invokeMethod('removeAdmin');
    } on PlatformException catch (e) {
      log("Failed to Invoke: '${e.message}'.");
    }
  }
}
