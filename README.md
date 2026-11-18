# KashafApp

Kashaf is a native Android community app for three clans: Red, Green, and Blue. Account creation and sign-in use Firebase Authentication, and the member profile is saved in Cloud Firestore. Clan and global chat use Firestore live listeners; admins can post global or clan-specific announcements.

## Free backend for a small launch

Firebase Authentication plus Cloud Firestore is a good fit for about 100 registered users and keeps the project in Java. The Firebase Spark plan currently includes Firestore's first database with 1 GiB stored data, 50,000 document reads/day, 20,000 writes/day, and 10 GiB/month outbound transfer. Exceeding a Spark quota can stop that product's service until its quota resets; watch usage in Firebase Console. These quotas can change, so check [Firebase pricing](https://firebase.google.com/pricing) before launch.

### Setup

1. Create a Firebase project and register the Android app with package name `com.max6778.kashafapp`.
2. The Firebase config file `app/google-services.json` is present in this repo for the registered Android app. It contains client project identifiers, not a server password; secure access with Firebase Authentication and Firestore Rules.
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

Promote admins by creating `admins/{uid}` in the Firebase Console, where `uid` is the account UID shown under Authentication → Users. The app checks this document before showing its announcement form, and Security Rules enforce admin writes. Never add an admin field to signup or let the app create admin documents. Decide who can remove messages and suspend accounts before opening chat to everyone.

## Build

GitHub Actions builds the debug APK on pushes and pull requests to `main`. From a completed workflow run, download the `KashafApp-debug` artifact. The project uses Java and Android Views.
