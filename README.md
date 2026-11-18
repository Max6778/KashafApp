# الكشفية الحسنية المغربية – فرع السلام

Kashfia Alhassania – Far3 Salam is a scout community app for the Far3 Salam branch. It uses Firebase Authentication for email/password accounts and Cloud Firestore for scout profiles, class chats, and announcements. Passwords are never saved to Firestore.

## Scout classes

| Arabic class | English label | Published age range | Accent | Tagline |
| --- | --- | --- | --- | --- |
| البراعم | Cubs | 5–7 | Gold | اكتشاف ومتعة |
| الأشبال والزهرات | Wolf Cubs & Brownies | 7–12 | Green | معًا نتعلّم وننمو |
| الكشافة والمرشدات | Scouts & Guides | 12–15 | Blue | خدمة ومهارة |
| الكشاف المتقدم والرائدات | Senior Scouts & Senior Guides | 15–18 | Purple | قيادة ومبادرة |
| الجوالة والدليلات | Rovers & Rangers | 18–23 | Red | مسؤولية وخدمة المجتمع |

These age bands follow the association's published 2018 internal regulations, Article 1. The official branch directory lists فرع السلام. The regulations also require a parent or guardian's signature for a minor's association enrollment; this app's account form is not a substitute for that enrollment process.

Members choose one class at registration. It sets their accent color and class chat. Class-specific announcements are visible only to that class, while announcements for all members are visible to everyone. Existing profiles from the old version are asked to select a scout class the next time they sign in.

## Features

- Email/password registration and sign-in with Firebase Authentication.
- Profiles store name, email, gender, scoutClass, rank, and createdAt. New profiles start at the member rank.
- One global room (`global`) and five private class rooms (`cubs`, `wolf_cubs_brownies`, `scouts_guides`, `senior_scouts_guides`, `rovers_rangers`). Admins can access all rooms.
- Text messages are limited to 1,000 characters. Photo/GIF attachments are limited to 700 KB. Photos are resized and JPEG-compressed on the device; GIF animation is preserved and oversized GIFs are rejected.
- Admins can publish announcements to everyone or a specific class, preview before publishing, and see all announcements. Announcements show audience, date, and author when available.
- Class emblems are included in class selection, class chat, home, and profile views. The existing launcher icon is retained.

## Firebase setup

1. Register the Android app using package name `com.max6778.kashafapp` and keep `app/google-services.json` in the repository.
2. Enable **Authentication → Sign-in method → Email/Password** and create the Firestore database.
3. Publish the current [`firestore.rules`](firestore.rules) from Firebase Console → Firestore Database → Rules. These rules restrict class chat access and announcement audiences, validate profile classes, and enforce message/media limits.
4. Promote an admin by creating `admins/{uid}` in Firestore Console, using the account UID from Authentication → Users. Admin documents must not be created by the app.

Firestore profiles only include the email address, not the password. Firebase Authentication handles password security.

## Build

GitHub Actions builds the debug APK on pushes and pull requests to `main`. Download the `KashafApp-debug` artifact from a successful workflow run. The app is implemented in Java with Android Views.
