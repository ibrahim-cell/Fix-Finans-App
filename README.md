
## V23.33 — Unified Fix Finans Logo
- Tek kanonik Fix Finans logosu web karşılama ekranı, uygulama ikonu ve platform varlıklarında kullanılır.
- Android `icon` ve `roundIcon` aynı `fix_finans_logo.png` varlığına bağlıdır.
- Karşılama ekranındaki logo ve ana uygulama `logoData` aynı logo ile eşitlendi.
# Fix Finans 13.3.10 — Android TWA

Fix Finans Android uygulaması Trusted Web Activity kullanır. Web arayüzü GitHub Pages üzerindeki Fix Finans uygulamasıdır.

Google/Firebase oturumu web tarafında çalışır. Karşılama ekranı, Google ile Devam Et ve Misafir olarak devam et davranışları web uygulamasından gelir.

Digital Asset Links fingerprint: F7:3C:8F:6A:FC:8E:50:D9:2E:58:78:64:59:8B:75:8D:8D:8C:A6:47:24:6E:D5:93:9F:F9:D3:3D:9A:66:05:DD

Build: compileSdk 36 / targetSdk 36 / Android Browser Helper 2.7.2.

Not: Test APK'sı debug keystore ile imzalanır. Play Store yayını öncesinde gerçek, gizli bir release keystore kullanılmalıdır.
## Marka / Android ikon
Android uygulaması Fix Finans'ın F + grafik + yükselen ok amblemini ana ikon olarak kullanır. Splash ekranı aynı premium kimliği ve “Kontrol sende. Denge sende.” mesajını taşır.


## V23.34 — Unified Welcome/Splash Logo
- Android uygulama ve yuvarlak uygulama ikonu `fix_finans_logo.png` kullanır.
- Karşılama ekranı (`index.html`) ve Android splash aynı kanonik şeffaf Fix Finans logosunu kullanır.


## Android WebView shell
This version uses a native Android WebView to host the published Fix Finans web app at the GitHub Pages URL. It includes Android back navigation, file chooser support, downloads, cookies/DOM storage, JavaScript, and external intent handling while preserving the existing web application code.
