# Fix Finans App — APK altyapısı 13.3.07

Bu proje, canlı Fix Finans PWA'sını Android'de Trusted Web Activity (TWA) olarak açan ince bir uygulama kabuğudur.

## Canlı adres
https://ibrahim-cell.github.io/Fix-Finans-App/

## Neden TWA?
Firebase + Google Authentication, gerçek Chrome/web ortamında çalıştığı için WebView yerine TWA/Custom Tabs yaklaşımı kullanılır. GoogleChrome Android Browser Helper 2.7.2 güncel yayımlanan sürümdür.  

## GitHub Actions ile APK
`main` dalına push edildiğinde `.github/workflows/build-apk.yml` debug APK üretir ve artifact olarak yükler.

## Yerel build
Android Studio veya JDK 17 + Android SDK 36 ile:

```bash
./gradlew assembleDebug
```

Çıktı:
`app/build/outputs/apk/debug/app-debug.apk`

## Önemli
TWA'nın tam ekran doğrulaması için, canlı web sitesinde `/.well-known/assetlinks.json` dosyası Android uygulamasının imza sertifikası SHA-256 parmak iziyle eşleşmelidir. İlk debug APK'sında doğrulama yoksa Chrome Custom Tab fallback'ına geçebilir; Firebase Google girişi yine gerçek web bağlamında çalışır.
