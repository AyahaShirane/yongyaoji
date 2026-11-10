# 用药记

用药记是一款离线 Android 用药安排与实际使用记录应用。你可以设置提醒，明确记录或跳过某次计划，撤销跳过，添加独立记录，查看历史记录，并根据已保存的记录查看汇总。提醒本身不代表已经用药。

- 应用 ID：`cn.medreminder.app`
- Android 8.0（API 26）或更高版本；目标 SDK 35
- 无需 Google Mobile Services
- 安排和记录保存在本机

应用默认使用英文界面；当 Android 系统语言为中文时会选择中文界面。无法识别的剂量格式仍会保留在记录中，但不会计入数值汇总。本应用用于记录，不提供医疗建议。请按照医护专业人员的指导填写药品和剂量信息。Android 通知和精确闹钟设置可能影响提醒投递。后台运行表现因设备和厂商而异，尚未在所有设备上验证。

## 构建

安装 Android SDK platform 35、build-tools 35.0.0 和 JDK 21，然后运行 `./build.sh`。默认生成未签名开发 APK：`dist/yongyaoji-debug-unsigned.apk`；签名后才能安装。第三方发布构建必须使用你自己控制的密钥库签名，并通过环境变量设置 `SIGNING_KEYSTORE`、`KEYSTORE_PASSWORD` 和 `KEY_PASSWORD`。项目不包含官方更新签名密钥。使用不同密钥签名的 APK 无法更新由官方密钥签名的安装。

项目根目录的 `LICENSE` 为 MIT 许可证副本，应用也会在 `res/raw/license_mit.txt` 中打包该文本。
