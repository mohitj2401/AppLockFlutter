import 'dart:convert';
import 'dart:typed_data';

List<ApplicationDataModel> applicationDataModelFromJson(String str) =>
    List<ApplicationDataModel>.from(
        json.decode(str).map((x) => ApplicationDataModel.fromJson(x)));

String applicationDataModelToJson(List<ApplicationDataModel> data) =>
    json.encode(List<dynamic>.from(data.map((x) => x.toJson())));

class ApplicationDataModel {
  ApplicationDataModel({
    this.isLocked,
    this.application,
  });

  bool? isLocked;
  ApplicationData? application;

  factory ApplicationDataModel.fromJson(Map<String, dynamic> json) =>
      ApplicationDataModel(
        isLocked: json["isLocked"],
        application: json["application"] == null
            ? null
            : ApplicationData.fromJson(json["application"]),
      );

  Map<String, dynamic> toJson() => {
        "isLocked": isLocked,
        "application": application == null ? null : application!.toJson(),
      };
}

class ApplicationData {
  ApplicationData({
    required this.appName,
    this.icon,
    required this.apkFilePath,
    required this.packageName,
    required this.versionName,
    required this.versionCode,
    required this.dataDir,
    required this.systemApp,
    required this.installTimeMillis,
    required this.updateTimeMillis,
    required this.category,
    required this.enabled,
  });

  String appName;
  Uint8List? icon;
  String apkFilePath;
  String packageName;
  String versionName;
  String versionCode;
  String dataDir;
  bool systemApp;
  String installTimeMillis;
  String updateTimeMillis;
  String category;
  bool enabled;

  factory ApplicationData.fromJson(Map<String, dynamic> json) {
    return ApplicationData(
      appName: json["appName"] ?? "",
      icon: json["icon"] == null ? null : base64Decode(json["icon"]),
      apkFilePath: json["apkFilePath"] ?? "",
      packageName: json["packageName"] ?? "",
      versionName: json["versionName"] ?? "",
      versionCode: json["versionCode"] ?? "",
      dataDir: json["dataDir"] ?? "",
      systemApp: json["systemApp"] ?? false,
      installTimeMillis: json["installTimeMillis"] ?? "",
      updateTimeMillis: json["updateTimeMillis"] ?? "",
      category: json["category"] ?? "",
      enabled: json["enabled"] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "appName": appName,
      "icon": icon == null ? null : base64Encode(icon!),
      "apkFilePath": apkFilePath,
      "packageName": packageName,
      "versionName": versionName,
      "versionCode": versionCode,
      "dataDir": dataDir,
      "systemApp": systemApp,
      "installTimeMillis": installTimeMillis,
      "updateTimeMillis": updateTimeMillis,
      "category": category,
      "enabled": enabled,
    };
  }
}
