<div align="center">
  <a href="https://githu.com"><img src="https://github.com/overphoenix/phoenix-android/media/phoenix-borders.png" width="200px"></a>
  <h1>Phoenix</h1>
  <p><i>just an android app for all occasions</i></p>
</div>

## How to build

1. Download Preview [Android Studio](https://developer.android.com/studio/preview)
2. Tools -> SDK Manager:
   - Download all SDK Platforms, beginning from Android 10.0 (Q).
   - SDK Build Tools (from 32.0.0); NDK (25.0.8221429); Android SDKCommand-line Tools; CMake; Android SDK Platform-Tools; Android SDK Tools;
3. Generate signing файл `app/config/release.jks` using [instruction](https://developer.android.com/studio/publish/app-signing).
4. Build project using Android Studio or from command line:
  1. `export JAVA_HOME=<path_to_android_studio>/jre`
  2. `./gradlew assembleRelease`
  3. Upadlo builded APK using adb:
    > adb push app/app/build/outputs/apk/release/phoenix-*.apk /storage/emulated/0/phoenix.apk
  4. Install it!

---

Copyright (C) 2022 Overphoenix

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.