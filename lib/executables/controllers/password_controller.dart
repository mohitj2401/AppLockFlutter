import 'dart:developer';
import 'dart:math' hide log;
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:app_lock_flutter/main.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:app_lock_flutter/executables/controllers/apps_controller.dart';
import 'package:app_lock_flutter/executables/controllers/method_channel_controller.dart';

import '../../services/constant.dart';

class PasswordController extends GetxController implements GetxService {
  SharedPreferences prefs;
  PasswordController({required this.prefs});
  bool isConfirm = false;
  String passcode = "";
  String addedPassCode = "";

  setPasscode(int index) {
    if (index == 10 && passcode.length < 6) {
      passcode = "${passcode}0";
    } else if (index == 11 && passcode.isNotEmpty) {
      List local = passcode.split("");
      local.removeLast();
      passcode = "";
      for (var element in local) {
        passcode = "$passcode$element";
      }
      log(passcode);
    } else if (index < 11 && passcode.length < 6) {
      passcode = "$passcode${index + 1}";
    }
    log("$passcode $index");
    update();
  }

  Future<void> savePasscode() async {
    if (!isConfirm) {
      if (passcode.isNotEmpty) {
        isConfirm = true;
        addedPassCode = passcode;
        passcode = "";
        update();
      } else {
        Fluttertoast.showToast(msg: "Invalid Passcode");
      }
    } else {
      if (addedPassCode == passcode) {
        // Generate secure random salt as hex string of 32 characters
        final random = Random.secure();
        final salt = List<int>.generate(16, (i) => random.nextInt(256))
            .map((e) => e.toRadixString(16).padLeft(2, '0'))
            .join();

        // Hash passcode + salt using SHA-256
        final bytes = utf8.encode(passcode + salt);
        final hash = sha256.convert(bytes).toString();

        // Store hash and salt in secure storage
        const storage = FlutterSecureStorage();
        await storage.write(key: AppConstants.setPassCode, value: hash);
        await storage.write(key: "passcode_salt", value: salt);

        // Remove from SharedPreferences if exists
        await prefs.remove(AppConstants.setPassCode);

        // Update AppsController hasPasscode state
        final appsController = Get.find<AppsController>();
        appsController.hasPasscode = true;
        appsController.update();

        // Update Native credentials
        await Get.find<MethodChannelController>().setPassword();

        navigatorKey.currentState!.pop();
      } else {
        Fluttertoast.showToast(msg: "passcode does not match");
      }
    }
  }

  Future<bool> verifyPasscode(String entered) async {
    const storage = FlutterSecureStorage();
    final storedHash = await storage.read(key: AppConstants.setPassCode);
    final salt = await storage.read(key: "passcode_salt");
    if (storedHash == null || salt == null) {
      return false;
    }
    final bytes = utf8.encode(entered + salt);
    final hash = sha256.convert(bytes).toString();
    return hash == storedHash;
  }

  clearData() {
    isConfirm = false;
    passcode = "";
    update();
  }
}
