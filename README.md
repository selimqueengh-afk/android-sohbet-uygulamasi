# Android Sohbet Uygulaması

Bu repo, basit bir Android sohbet uygulaması geliştirmek için oluşturulmuştur.

## Düzeltilen Sorunlar

✅ **Gradle Konfigürasyonu:**
- Proje düzeyinde `build.gradle` dosyası eklendi
- `gradle.properties` dosyası oluşturuldu
- Gradle wrapper dosyaları (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) eklendi

✅ **Android Projesi:**
- `app/build.gradle` dosyası güncellenedgi, SDK sürümleri ve dependencies düzeltildi
- `AndroidManifest.xml` modern Android namespace kullanımına güncellendi
- Eksik Activity (`MainActivity.kt`) oluşturuldu
- Layout dosyaları (`activity_main.xml`, `item_message_user.xml`, `item_message_other.xml`) düzeltildi
- Gerekli drawable'lar ve resource'lar eklendi

✅ **GitHub Actions:**
- Android CI workflow dosyası optimize edildi
- Android SDK kurulumu eklendi
- Build steps iyileştirildi (lint, debug/release build, test)
- Artifact upload işlemleri düzenlendi

## Özellikler
- Kullanıcı arayüzü (mesaj yazma ve gönderme)
- Mesaj listesinin RecyclerView ile gösterilmesi
- Kullanıcı ve diğer mesajlar için farklı tasarım
- Material Design temalar
- Kotlin ile geliştirildi

## Kurulum

### Yerel Geliştirme
1. Bu repoyu klonlayın:
   ```bash
   git clone [repo-url]
   cd android-sohbet-uygulamasi
   ```

2. Android Studio ile açın veya komut satırından build edin:
   ```bash
   ./gradlew assembleDebug
   ```

3. APK dosyası `app/build/outputs/apk/debug/` klasöründe oluşacaktır.

### GitHub Actions
- Her push ve pull request'te otomatik build çalışır
- Debug ve Release APK'lar artifact olarak yüklenir
- Lint raporları otomatik oluşturulur

## Proje Yapısı
```
app/
├── src/main/
│   ├── java/com/selimqueengh/sohbet/
│   │   ├── MainActivity.kt
│   │   ├── Message.kt
│   │   └── MessageAdapter.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── mipmap/
│   └── AndroidManifest.xml
├── build.gradle
└── proguard-rules.pro
```

## Teknolojiler
- **Dil:** Kotlin
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Build Tool:** Gradle 8.2
- **UI:** Material Design Components
- **RecyclerView** için mesaj listesi

## Katkı
Katkıda bulunmak için pull request gönderebilirsiniz. Lütfen GitHub Actions build'lerinin başarılı olduğundan emin olun.