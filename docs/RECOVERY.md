# Account recovery

Open the account icon in the app header, then **Sign in with Google**. A new
Google account links to the current anonymous Firebase UID, preserving its SQL
ownership and local visits. Sign-in also downloads that account's cloud history.
After reinstalling, use the same Google account. **Restore from cloud** retries
the download without signing in again.

The Android Google OAuth client must match the APK's signing certificate and
package name. Google must be enabled in Firebase Authentication, and the updated
`app/google-services.json` must contain the web OAuth client ID. The supplied
`scripts/configure-google.cjs` previews configuration; `--apply` enables Google
for the existing Android app and downloads its configuration. It uses this
developer machine's debug certificate fingerprints. Register the correct
certificate before distributing builds signed by a different key.

## Recover the old anonymous installation

This requires a Firebase project administrator. A UID is an identifier, not a
password: the app cannot claim another account simply by entering its old UID.
No administrator credential is included in the app, and client SQL queries
always restrict ownership to the authenticated UID.

1. Sign in with Google in the updated app. Copy the **Account ID for recovery**.
2. As administrator, inspect the old owner summaries and save a private backup:

   ```powershell
   New-Item -ItemType Directory recovery-private -Force
   node scripts/recover-account.cjs owners
   node scripts/recover-account.cjs backup OLD_UID recovery-private/original.json
   ```

3. Confirm which source history belongs to the person requesting recovery.
   Generate a plan using their Google-linked Firebase UID:

   ```powershell
   node scripts/recover-account.cjs plan OLD_UID GOOGLE_UID recovery-private/plan.json
   ```

   The script verifies the destination is an enabled Google-linked account and
   reports its email and the number of missing active visits. Review these before
   copying. The plan also contains a complete source snapshot, including deletion
   markers. These files contain private visit data; keep them out of Git and off
   shared storage. `recovery-private/` is ignored by Git.

4. Apply the reviewed plan:

   ```powershell
   node scripts/recover-account.cjs apply recovery-private/plan.json
   ```

   This copies missing active visits with their original IDs, timestamps, notes,
   types, and revisions. Original SQL records are untouched. Existing destination
   IDs, including deletion markers, are never overwritten. The source digest and
   destination Google account are rechecked before writing. Insert-only batches
   are atomic; after an interrupted run, repeat the same command to skip completed
   IDs. If the source changed, generate a fresh plan.

5. Tap **Restore from cloud** in the app. The count reports additional active
   visits inserted locally; updates to existing visits do not increase that count.

The scripts use a logged-in official Firebase CLI installed at
`~/.cache/firebase/tools/lib/node_modules/firebase-tools`. For another installation,
set `COUNTER_FIREBASE_TOOLS` to its `firebase-tools` package directory. Log in using
`firebase login` first. Credentials stay in the CLI's credential store. Nothing
in these scripts deletes accounts or original SQL records.

## Restore behavior and limits

- The app fetches all pages, including cloud deletion markers, before one local
  transaction. A failed page leaves the local history unchanged.
- New cloud rows enter the local database as already synced. Existing clean rows
  accept equal/newer cloud revisions. Unsynced local edits and deletions win.
  Their revisions advance above the downloaded cloud version before retrying
  upload, and rejected revision conflicts are never marked synced.
- Local writes, uploads, account changes, and restore are serialized to prevent
  switching identities during an upload or applying a restore halfway through an edit.
- Switching a nonempty local database to a different existing Google account is
  refused. Administrator-assisted merging is required; no automatic reassignment
  of local visits occurs.
- This is account recovery, not a full multi-device conflict-resolution system.
  Use one installation for editing at a time. Restore is explicit (and runs after
  Google sign-in); it is not a continuous cloud subscription.
- Visits that never reached SQL cannot be recovered after local storage is lost.

## Safe verification and installation

Never run `connectedDebugAndroidTest` on a phone containing real visits: test
tooling can replace/uninstall the target app. Use the emulator-only guard:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug -PcounterSkipBidiLint=true
.\scripts\test-android-emulator.ps1 -Serial emulator-5554
node --test --test-isolation=none scripts/recover-account.test.cjs
```

Run `scripts/test-dataconnect.ps1` against the local demo SQL Connect emulator for
authorization, Google/anonymous access, 501-row pagination, deletion markers, and
the administrator insert mutation. It never calls production.

Install updates over the existing app with the same signing key. Do not uninstall
or clear storage to resolve an installation error; inspect the error first.
