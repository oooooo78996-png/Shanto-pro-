# SHANTO TRADER (বাংলা নির্দেশনা)

⚠ এই প্রজেক্ট আমি কম্পাইল করে টেস্ট করতে পারিনি (এখানে Android SDK/ইন্টারনেট ছিল না)। GitHub Actions বিল্ড ফেল করলে লগের এরর আমাকে পাঠান, ঠিক করে দেব।

## GitHub-এ আপলোড (ফোন থেকে)
1. ZIP ফাইলটা ফোনে Extract করুন (Files অ্যাপ → ZIP চেপে ধরুন → Extract)।
2. github.com এ লগইন → উপরে "+" → "New repository" → নাম `ShantoTrader` → "Create repository"।
3. "uploading an existing file" চাপুন → Extract করা ফোল্ডারের ভেতরের সব ফাইল/ফোল্ডার (app, build.gradle.kts, settings.gradle.kts, gradle.properties) সিলেক্ট করে আপলোড করুন → "Commit changes"।
4. ⚠ GitHub ওয়েব আপলোডে `.github` (ডট ফোল্ডার) অনেক সময় বাদ পড়ে। তাই: "Add file" → "Create new file" → নামের ঘরে লিখুন `.github/workflows/build.yml` → ZIP-এর ভেতরের build.yml এর লেখা কপি-পেস্ট করুন → "Commit changes"।

## APK বিল্ড
1. রিপোজিটরির "Actions" ট্যাব → "Build Debug APK" → "Run workflow" (বা কমিট করলেই নিজে চলবে)।
2. সবুজ টিক হলে ওই রান খুলুন → নিচে "Artifacts" → `ShantoTrader-debug-apk` ডাউনলোড (ZIP) → Extract → `app-debug.apk`।
3. লাল ✗ হলে রান খুলে ফেল হওয়া ধাপের লেখা কপি করে আমাকে পাঠান।

## ইনস্টল
APK ট্যাপ করুন → "Install unknown apps" চাইলে Settings থেকে অনুমতি দিন → Install।
বাংলা ভয়েসের জন্য: Settings → Text-to-speech → Google → বাংলা ভয়েস ডাউনলোড।
