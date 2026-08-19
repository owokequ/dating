# For my L mobile

Native Expo client for Owoke. It uses the public HTTPS API Gateway and is not part of Docker Compose.

## Local run

1. Copy `.env.example` to `.env.local` and set `EXPO_PUBLIC_API_URL` to a reachable HTTPS gateway; `localhost` does not work from a phone.
2. Run `npm ci` and `npm start`.
3. Scan the QR code in Expo Go for layout and ordinary email login.

Run checks with `npm run lint`, `npm run typecheck`, and `npm test`.

## EAS builds and push

The identifier is `app.owoke.formyl`; the custom scheme is `formyl`; the EAS project ID is in `app.json`.

For Android push, download `google-services.json` from Firebase Console → Project settings → General → Your apps, and put it in this directory. The Expo config includes it automatically in EAS builds, while Expo Go continues to work without it. Create a Firebase FCM V1 service-account JSON in Firebase Console → Project settings → Service accounts, then upload that private file to Expo/EAS credentials; never commit it. Finally enable Expo enhanced push security and put its access token only in the backend deployment secret `EXPO_PUSH_ACCESS_TOKEN`; set `EXPO_PUSH_ENABLED=true` there.

Remote push and Telegram login callbacks require an EAS development/preview/production build, not Expo Go. Register `formyl://auth/telegram` as the native callback. After an Android build, get its SHA-256 signing fingerprint from EAS and replace the placeholder in `web-app/public/.well-known/assetlinks.json` before deploying the website.

iOS is deliberately deferred: create an Apple Developer account, configure APNs in Expo/EAS, build through TestFlight, and serve `apple-app-site-association` from the web domain before enabling Universal Links.
