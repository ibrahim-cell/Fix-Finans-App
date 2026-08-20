# Fix Finans 13.3.10 — Android TWA

Fix Finans Android uygulaması Trusted Web Activity kullanır. Web arayüzü GitHub Pages üzerindeki Fix Finans uygulamasıdır.

Google/Firebase oturumu web tarafında çalışır. Karşılama ekranı, Google ile Devam Et ve Misafir olarak devam et davranışları web uygulamasından gelir.

Digital Asset Links fingerprint: F7:3C:8F:6A:FC:8E:50:D9:2E:58:78:64:59:8B:75:8D:8D:8C:A6:47:24:6E:D5:93:9F:F9:D3:3D:9A:66:05:DD

Build: compileSdk 36 / targetSdk 36 / Android Browser Helper 2.7.2.

Not: Test APK'sı debug keystore ile imzalanır. Play Store yayını öncesinde gerçek, gizli bir release keystore kullanılmalıdır.


## 20.09 — Android/TWA doğrulama notu

Bu sürüm TWA yapılandırmasını temizler ve `asset_statements` içinde web origin'ini kullanır.

**Önemli:** Digital Asset Links doğrulaması web origin'inin kökündeki şu adresten yapılır:
`https://ibrahim-cell.github.io/.well-known/assetlinks.json`

GitHub Pages proje sitesi `https://ibrahim-cell.github.io/Fix-Finans-App/` olduğu için yalnızca proje klasörü içindeki `.well-known/assetlinks.json` dosyası kök-origin doğrulamasını garanti etmez. TWA'nın Chrome UI olmadan doğrulanmış şekilde açılması için aynı `assetlinks.json` içeriğinin GitHub kullanıcı Pages kök sitesinde de yayınlanması veya bir özel alan adının kökünde yayınlanması gerekir.

Bu ZIP mevcut Fix Finans web arayüzüne ve hesaplama koduna dokunmaz.
