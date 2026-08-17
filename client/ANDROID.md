# Shipping the Android app

The project is managed (no `android/` directory), so the APK is produced by EAS Build, Expo's cloud
service, and distributed as a GitHub Release asset.

---

## Why the config looks the way it does

**`android.package` is `com.adityapareek.socialmediaapp`.** Android identifies an app by this string,
not by its name. Change it and you have a different app: it installs alongside the old one instead
of updating it, and on the Play Store the id can never be reused. **Change it now if you want
something else** — after the first install anywhere, you are stuck with it.

**`usesCleartextTraffic` is gone.** It was there so a physical Android device could reach a
development server over plain HTTP. In a release build it means the app silently accepts
unencrypted connections, which is pointless now that the API is HTTPS behind CloudFront. Android 9+
blocks cleartext by default, which is what we want. Expo Go permits cleartext itself, so local
development is unaffected.

**`preview` builds an APK, `production` builds an AAB.** This is the distinction that trips people
up: an `.aab` is an upload format for the Play Store and **cannot be installed on a phone**. Only
the APK is sideloadable, which is what makes GitHub Releases work as a channel.

**`appVersionSource: "remote"`** lets EAS own the Android `versionCode`, and `autoIncrement` on the
production profile bumps it per build. Play Store rejects an upload whose versionCode it has seen
before, and tracking that by hand is a reliable way to waste an afternoon.

---

## One-time setup

```bash
cd client
npm install                       # picks up eas-cli
npx eas login                     # create an account at expo.dev first
npx eas init                      # links the project, writes extra.eas.projectId to app.json
```

Then put the real API URL in **both** profiles in `eas.json`, replacing `REPLACE-ME`:

```json
"env": { "EXPO_PUBLIC_API_URL": "https://d1234abcd.cloudfront.net" }
```

`EXPO_PUBLIC_*` values are compiled into the bundle, so an APK is permanently pinned to whatever URL
it was built with. There is no runtime override — a new URL means a new build.

---

## Build an installable APK

```bash
npx eas build -p android --profile preview
```

The first run asks to generate an Android keystore. Say yes; EAS stores it.

**Back it up straight away:**

```bash
npx eas credentials
```

Losing the keystore means you can never update an installed app again — users would have to
uninstall and reinstall, losing their session. This is the one unrecoverable mistake available here.

The build queues (free tier is low priority, so this can take a while), then prints a download URL.

---

## Publish it on GitHub

Do **not** commit the APK. Binaries stay in git history permanently and cannot be pruned without
rewriting it. Attach it to a Release instead:

```bash
gh release create v1.0.0 ./social-media-app.apk \
  --title "v1.0.0" \
  --notes "First Android build. Sideload: download, open, allow installs from this source."
```

Installing: download on the phone, open it, and approve "install unknown apps" for the browser when
prompted. Play Protect will show a warning for an unsigned-by-Play app — expected for sideloading.

---

## Testing without a build

`npx expo start` and scan the QR code with Expo Go. Every dependency here is Expo-maintained, so it
runs. The one catch is that `EXPO_PUBLIC_API_URL` must be reachable *from the phone*: the CloudFront
URL works, `localhost` does not (it means the phone itself), and a LAN address only works while the
server is running on your machine and reachable over HTTP.

---

## Things to know before you rely on this

**No over-the-air updates.** `expo-updates` is not installed, so every change — including a one-line
fix — means a new APK that every user downloads and installs by hand. Adding `expo-updates` later
lets you push JS-only changes instantly; native changes still need a rebuild.

**The free tier allows 15 Android builds per month** on low-priority queues. Enough, but not
unlimited, which is the reason not to wire up a GitHub Action that builds on every push.

**CORS does not apply here.** A native app sends no `Origin` header, so `CORS_ALLOWED_ORIGINS` on
the server governs the web build only. The Android app talks to the same API with no CORS involved.

**iOS needs an Apple Developer account** ($99/year) even for on-device testing beyond a 7-day
sideload. The `production` profile above is Android-shaped; iOS would need its own.
