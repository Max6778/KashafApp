# KashafApp

Kashaf is a native Android community app for three clans: Red, Green, and Blue. Account creation and sign-in use Firebase Authentication, and the member profile is saved in Cloud Firestore. The announcements and chat screens are still visual previews and are not connected to Firestore yet.

## Free backend for a small launch

Firebase Authentication plus Cloud Firestore is a good fit for about 100 registered users and keeps the project in Java. The Firebase Spark plan currently includes Firestore's first database with 1 GiB stored data, 50,000 document reads/day, 20,000 writes/day, and 10 GiB/month outbound transfer. Exceeding a Spark quota can stop that product's service until its quota resets; watch usage in Firebase Console. These quotas can change, so check [Firebase pricing](https://firebase.google.com/pricing) before launch.

### Setup

1. Create a Firebase project and register the Android app with package name `com.max6778.kashafapp`.
2. Download `google-services.json` into the project’s `app/` folder. It contains project identifiers, not a server secret; this repo ignores it so each checkout can use its own Firebase project configuration.
3. Enable **Authentication → Sign-in method → Email/Password** and create the Firestore database in production mode.
4. The Firebase Google Services plugin, Auth, and Firestore SDK dependencies are already added. After placing `google-services.json` in `app/`, sync Gradle. The signup form collects name, email, password, gender, and clan; password handling is done by Firebase Authentication and the app never writes it to Firestore or local preferences.
5. Review and publish the starter [Firestore rules](firestore.rules) before releasing the app. Treat client-side admin checks as display logic only; enforce admin permissions in Security Rules.

### Suggested Firestore model

- `users/{uid}`: `name`, `gender`, `clan` (`red`, `green`, or `blue`), `rank` (starts as `member`), and `createdAt`. A user can edit their name and gender but cannot change their clan or rank. Only admins can assign ranks. Keep profile reads private unless the app needs a public profile.
- `admins/{uid}`: an admin marker document. Create these only from the Firebase Console or a trusted Admin SDK environment, never from the app.
- `announcements/{announcementId}`: `title`, `body`, `createdBy`, `createdAt`, and `audience` (`all` or a clan). Signed-in users can read; only admins can create, edit, or delete.
- `rooms/{roomId}/messages/{messageId}`: `authorId`, `authorName`, `text`, and `createdAt`. Use one `global` room and one room per clan (`red`, `green`, `blue`). Rules should allow reads/writes for the global room to signed-in users, and clan-room access only when the caller's `users/{uid}.clan` matches that room. Enforce message length and ownership in the rules.

Use Firestore snapshot listeners for live chat updates, paginate message history, and avoid loading an unbounded room. With 100 users, these limits are normally ample for a modest community, but chat listeners count document reads as messages arrive. Set usage alerts and review Firestore usage regularly.

## Admins and moderation

Promote admins by adding their UID to `admins/{uid}` from the Firebase Console. Rules should check that document for announcement writes. Avoid a public sign-up field or local preference that lets a user set their own admin status. Decide who can remove messages and suspend accounts before opening chat to everyone.

## Build

Open this folder in Android Studio and run the `app` configuration. The project uses Java and Android Views. Each checkout needs its own `app/google-services.json` file, which is intentionally excluded from Git.
