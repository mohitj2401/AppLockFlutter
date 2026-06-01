import 'dart:developer';
import 'dart:typed_data';
import 'package:installed_apps/installed_apps.dart';
import 'package:installed_apps/app_info.dart';
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:app_lock_flutter/executables/controllers/method_channel_controller.dart';
import 'package:app_lock_flutter/services/constant.dart';
import '../../models/application_model.dart';

class AppsController extends GetxController implements GetxService {
  SharedPreferences prefs;
  AppsController({required this.prefs});

  String? dummyPasscode;
  int? selectQuestion;
  TextEditingController typeAnswer = TextEditingController();
  TextEditingController checkAnswer = TextEditingController();
  TextEditingController searchApkText = TextEditingController();
  List<AppInfo> unLockList = [];
  List<ApplicationDataModel> searchedApps = [];
  List<ApplicationDataModel> lockList = [];
  List<String> selectLockList = [];
  bool addToAppsLoading = false;
  bool hasPasscode = false;

  List<String> excludedApps = [];

  int appSearchUpdate = 1;
  int addRemoveToUnlockUpdate = 2;
  int addRemoveToUnlockUpdateSearch = 3;

  @override
  void onInit() {
    super.onInit();
    checkPasscodeSaved();
  }

  Future<void> checkPasscodeSaved() async {
    const storage = FlutterSecureStorage();
    String? hash = await storage.read(key: AppConstants.setPassCode);
    hasPasscode = hash != null && hash.isNotEmpty;
    update();
  }

  changeQuestionIndex(index) {
    selectQuestion = index;
    update();
  }

  resetAskQuetionsPage() {
    selectQuestion = null;
    typeAnswer.clear();
    checkAnswer.clear();
  }

  getPasscode() {
    return "";
  }

  Future<void> removePasscode() async {
    const storage = FlutterSecureStorage();
    await storage.delete(key: AppConstants.setPassCode);
    await storage.delete(key: "passcode_salt");
    await prefs.remove(AppConstants.setPassCode);
    hasPasscode = false;
    update();
  }

  setSplash() {
    prefs.setBool("Splash", true);
    return prefs.getBool("Splash");
  }

  getSplash() async {
    final prefs = await SharedPreferences.getInstance();
    if ((prefs.getBool("Splash")) != null) {
      return true;
    } else {
      return false;
    }
  }

  excludeApps() {
    for (var e in excludedApps) {
      unLockList.removeWhere((element) => element.packageName == e);
    }
  }

  getAppsData() async {
    unLockList = await InstalledApps.getInstalledApps(
      excludeSystemApps: false,
      withIcon: true,
    );
    excludeApps();
    update();
  }

  addRemoveFromLockedAppsFromSearch(ApplicationData app) {
    addToAppsLoading = true;
    update();
    try {
      if (selectLockList.contains(app.appName)) {
        selectLockList.remove(app.appName);
        lockList.removeWhere(
            (element) => element.application!.appName == app.appName);
      } else {
        if (lockList.length < 16) {
          selectLockList.add(app.appName);
          lockList.add(
            ApplicationDataModel(
              isLocked: true,
              application: ApplicationData(
                apkFilePath: app.apkFilePath,
                appName: app.appName,
                category: app.category,
                dataDir: app.dataDir,
                enabled: app.enabled,
                icon: getAppIcon(app.appName),
                installTimeMillis: app.installTimeMillis,
                packageName: app.packageName,
                systemApp: app.systemApp,
                updateTimeMillis: app.updateTimeMillis,
                versionCode: app.versionCode,
                versionName: app.versionName,
              ),
            ),
          );
        } else {
          Fluttertoast.showToast(
              msg: "You can add only 16 apps in locked list");
        }
      }
    } catch (e) {
      log("-------$e", name: "addRemoveFromLockedAppsFromSearch");
    }
    addToAppsLoading = false;
    update();
  }

  addToLockedApps(AppInfo app, context) async {
    addToAppsLoading = true;
    update([addRemoveToUnlockUpdate]);
    try {
      if (selectLockList.contains(app.name)) {
        selectLockList.remove(app.name);
        lockList.removeWhere((em) => em.application!.appName == app.name);
        log("REMOVE: $selectLockList");
      } else {
        if (lockList.length < 16) {
          selectLockList.add(app.name);
          lockList.add(
            ApplicationDataModel(
              isLocked: true,
              application: ApplicationData(
                apkFilePath: "",
                appName: app.name,
                category: "",
                dataDir: "",
                enabled: true,
                icon: app.icon ?? Uint8List(0),
                installTimeMillis: "",
                packageName: app.packageName,
                systemApp: false,
                updateTimeMillis: "",
                versionCode: '${app.versionCode}',
                versionName: '${app.versionName}',
              ),
            ),
          );
          log("ADD: $selectLockList", name: "addToLockedApps");
          Get.find<MethodChannelController>().addToLockedAppsMethod();
        } else {
          Fluttertoast.showToast(
              msg: "You can add only 16 apps in locked list");
        }
      }
    } catch (e) {
      log("-------$e", name: "addToLockedApps");
    }
    prefs.setString(
        AppConstants.lockedApps, applicationDataModelToJson(lockList));
    addToAppsLoading = false;
    update([addRemoveToUnlockUpdate]);
  }

  getLockedApps() {
    try {
      lockList = applicationDataModelFromJson(
          prefs.getString(AppConstants.lockedApps) ?? '');
      selectLockList.clear();
      log('${lockList.length}', name: "STORED LIST");
      for (var e in lockList) {
        selectLockList.add(e.application!.appName);
      }
      log('${lockList.length}-$selectLockList', name: "Locked Apps");
    } catch (e) {
      log("-------$e", name: "getLockedApps");
    }

    update();
  }

  Uint8List getAppIcon(String appName) {
    final idx = unLockList.indexWhere((element) => appName == element.name);
    if (idx != -1) {
      return unLockList[idx].icon ?? Uint8List(0);
    }
    return Uint8List(0);
  }

  appSearch() {
    searchedApps.clear();
    if (searchApkText.text.isNotEmpty) {
      for (var e in unLockList) {
        if (e.name
            .toUpperCase()
            .contains(searchApkText.text.toUpperCase().trim())) {
          searchedApps.add(
            ApplicationDataModel(
              isLocked: null,
              application: ApplicationData(
                apkFilePath: "",
                appName: e.name,
                category: "",
                dataDir: "",
                enabled: true,
                icon: e.icon,
                installTimeMillis: "",
                packageName: e.packageName,
                systemApp: false,
                updateTimeMillis: "",
                versionCode: '${e.versionCode}',
                versionName: '${e.versionName}',
              ),
            ),
          );
        }
      }
    }
    update([appSearchUpdate]);
  }
}
