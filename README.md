Fix Finans 13.2.85 — 4.5 Net Bakiye Referans Tasarım Dili

Net Bakiye kartının mevcut cüzdanı ve yerleşimi korunarak kart zemini, kenar tonu ve kontrollü mor ışık referans görsele yaklaştırıldı. Cüzdan bozulmadı; mor atmosfer yalnızca cüzdan çevresinde ve kartın sağ tarafında hafif tutuldu.

13.2.87 — Eski cüzdan CSS override/kalıntıları temizlendi; Net Bakiye kartı ve gerçekçi cüzdan için tek, güncel CSS katmanı bırakıldı.


## 13.2.89 — Firebase / Google Authentication
- Existing Fix Finans Firebase project configuration connected to the web app.
- Google provider is used through Firebase Authentication.
- Profile screen includes Google account connection status and connect/disconnect action.
- On successful Google sign-in, display name and email are synchronized to the local profile.
- Existing wallet, profile photo, Net Bakiye and reference UI are preserved.
- For GitHub Pages, the deployed domain must be added under Firebase Authentication > Settings > Authorized domains.
