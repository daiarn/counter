# counter

An Android app for logging poop, pee, and combined bathroom visits.
The agreed requirements are in [SPECIFICATION.md](SPECIFICATION.md).

## Current implementation

The interface follows the supplied learning-app references: deep blue backgrounds,
slate blue cards, warm gold accents, rounded navigation, and consistent outline icons.
The poop launcher icon uses a matching gold background. The palette stays blue
in both system light and dark modes.
Phone wallpaper colors do not override the app's palette.

- Three buttons save timestamped visits immediately to a Room database.
- Today provides date selection, totals, an hourly poop/pee bar chart, and visits.
- History provides calendar and daily totals; selecting a day opens it in Today.
- Records shows all-time daily and calendar-month highs for poop and pee, with
  tied-period counts and the most recent matching date/month. Both contributes to
  each category; deleted visits are excluded and dates use the phone's time zone.
- Add forgotten visits, edit type/time/notes, and delete with confirmation.
- Future manual timestamps are rejected. Repeated daylight-saving times require
  choosing an offset; nonexistent local times are rejected.
- History follows the phone's current time zone and shows elapsed time since the
  latest poop/pee across all days.
- Local changes upload through WorkManager when connected, with retry/backoff.
- Anonymous Firebase authentication identifies an installation without a login screen.
- The account icon offers Google sign-in and cloud restore. Linking keeps the
  installation's identity; signing into the same Google account after reinstalling
  restores its cloud history. Old anonymous histories require an administrator
  recovery copy; see [Recovery](docs/RECOVERY.md).
- Revision checks preserve corrections made during an upload; deletion markers prevent
  delayed uploads from restoring deleted visits.

Firebase configuration is present for `counter-f0787`, and the Google services
Gradle plugin is wired up. The Android authentication and upload code is implemented.
The SQL Connect schema and `counter` connector are deployed in `europe-central2`.
Local logging continues if authentication or cloud requests fail.
The live Android SDK test passed for anonymous authentication, upload/edit/delete,
owner isolation, and delayed/duplicate revisions.
An emulator check also confirmed a first offline Both visit stays pending and then
uploads through WorkManager after connectivity is restored.

SQL Connect details supplied by the owner:

- Service: `counter-f0787-service`
- Region: `europe-central2`
- Cloud SQL instance: `counter-f0787-instance` (verified through the Firebase CLI)
- PostgreSQL database: `counter-f0787-database`
- Connector: `counter` (deployed)

The schema and owner-scoped operations are in `dataconnect/`. The client uses the
generated Firebase Kotlin SDK in `app/src/main/java/com/daiarn/counter/generated/`. Only deployed named
operations can run; the app does not send arbitrary GraphQL to production.
Creation is insert-only; updates require a greater revision inside the SQL filter.
Deleted rows retain a revision marker and an empty note. Cloud restore is available
for Google-linked accounts; full multi-device conflict resolution is not implemented.
Android backup/device transfer excludes
the visits database and preferences to keep records tied to the installation identity.

## Firebase deployment

Firebase Authentication's Anonymous sign-in provider is enabled. For future schema
changes, install/login to the Firebase CLI, then run from this project directory:

```powershell
firebase login
firebase dataconnect:services:list --project counter-f0787
firebase dataconnect:sql:diff --project counter-f0787
firebase dataconnect:sql:migrate --project counter-f0787
firebase deploy --only dataconnect --project counter-f0787
firebase dataconnect:sdk:generate --project counter-f0787
```

Verify the Cloud SQL instance/database names in `dataconnect/dataconnect.yaml` before
deployment. Review the migration shown by the CLI; compatible mode is configured.
`USER_ANON` and server-derived `auth.uid` scope every operation to its owner.
After deployment, test first-launch offline logging, reconnection, edits/deletions,
and denial of another user's access. Do not enable automatic anonymous-account cleanup
while visits depend on these identities. Use the account recovery workflow above
if anonymous credentials have already been lost.

On this computer, the official CLI was downloaded to `app/build/tools/firebase.exe`.
Use `scripts/firebase.ps1` in place of `firebase` to avoid its Windows welcome launcher.
The executable is ignored by Git and can be downloaded again from the
[official Firebase CLI installation guide](https://firebase.google.com/docs/cli).

Local server regression checks (synthetic users, no production access):

```powershell
.\scripts\firebase.ps1 emulators:start --only dataconnect --project demo-counter
# In another terminal:
.\scripts\test-dataconnect.ps1
```

The opt-in live Android SDK test creates disposable anonymous users and synthetic
visits, then deletes those users and leaves only a synthetic deletion marker:

```powershell
$env:ANDROID_SERIAL = 'emulator-5554' # Use only an emulator, never a phone with real visits.
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.firebaseLive=true' '-Pandroid.testInstrumentationRunnerArguments.class=com.daiarn.counter.sync.FirebaseCloudTest'
```

## Run

Open this directory in Android Studio, allow Gradle to sync, choose an emulator or
phone running Android 16 or newer, and run the `app` configuration. The application
ID is `com.daiarn.counter`, and the minimum SDK is API 36.

For PowerShell builds on this computer:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Verify

```powershell
.\gradlew.bat testDebugUnitTest
.\scripts\test-android-emulator.ps1
.\gradlew.bat lintDebug
```

Emulator tests cover Room persistence, corrections, future-time validation, and
quick logging with deletion confirmation. Unit tests cover totals, date/time rules,
failed uploads, and edits/deletions during uploads.
The UI/Room regression suite also passed on the connected Android 16 Samsung phone
and the API 37 emulator after the visual refresh. Live Firebase integration was
verified separately on the emulator; manual phone acceptance checks remain pending.

The current AGP 9.4.1 lint run stalled inside `BidiSpoofing` while walking Kotlin
comments, as observed in a thread dump. For this toolchain, an opt-in workaround is:

```powershell
.\gradlew.bat lintDebug -PcounterSkipBidiLint=true
```

This skips only that detector; ordinary lint still enables it. A separate source
scan found no bidirectional Unicode controls. Recheck normal lint after updating
the affected tooling. Local server tests cover anonymous access, owner isolation,
deletion, stale/duplicate revisions, and unauthenticated denial. They complement the
opt-in live Android SDK test; they do not replace phone and lifecycle validation.

