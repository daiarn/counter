# Counter — Android app specification

Status: Initial product requirements agreed; ready for implementation planning.
Updated: 2026-10-25
Repository: https://github.com/daiarn/counter

## Purpose

Create an Android app for recording how many times a day a person poops and pees.
Each entry represents one bathroom visit and includes its date and time.

## Confirmed requirements

### Logging

- Three buttons: **Poop**, **Pee**, and **Both**.
- Tapping a logging button saves immediately using the current time.
- Save the date and time of every visit.
- Both records one visit and increases both daily totals by one.
- Support an optional note on each visit.
- Add or edit notes by opening a visit from Today; quick logging has no note prompt.
- Users can add forgotten visits in the past.
- Users can edit a visit's type, date/time, and note, or delete the visit.
- Deletion requires confirmation; deletion Undo is not required.
- Future dates/times are blocked when adding or editing entries.
- A day runs from midnight to midnight using the phone's local time.
- Display and group all visits in the phone's current time zone. When that zone
  changes, convert existing timestamps into the new zone and recalculate daily
  totals; a visit may move to another calendar day. Its actual instant stays unchanged.

### Home screen

- Focus on the three logging buttons.
- Totals, visits, and time since the last poop/pee belong on Today.
- Exact layout will be developed after functionality works.

### Today tab

- Show the selected day's separate poop/pee totals, hourly chart, and individual visits.
- Start with today. Provide a date picker, previous/next-day buttons, and Go to today.
- Block future date selection. Empty dates show zero totals and no visits.
- Follow the new date at midnight when viewing today; keep a deliberately selected past date.
- Display 24 local clock-hour groups with separate poop and pee bars. Both adds one to each.
- Repeated daylight-saving hours share a bucket; nonexistent hours show zero.
- On today, show time since each type's latest visit across all days; use Not recorded yet if absent.
- On past dates, show each type's last time on that date, or None.
- Support adding, editing, and confirmed deletion from the selected day's visit list.

### History tab

- Provide a calendar with separate poop/pee totals and a daily totals list.
- Include zero-visit dates between the earliest recorded visit and today.
- With no visits, the list contains today with zero totals.
- Selecting a calendar day or daily row opens its details and chart in Today.

### Records tab

- Show all-time highest daily poop and pee totals separately, with the date.
- When multiple days share a record, show the number of tied days and the most recent date.
- Show the calendar month and year with the most poop and pee separately, with totals.
- For monthly ties, show the number of tied months and the most recent month.
- Both contributes to both categories; deleted visits are excluded.
- Use the phone's current time zone and recalculate when saved visits change.
- Show “Not recorded yet” for categories with no recorded visits.

### Name and appearance

- App name: **counter**.
- Use a minimal visual style initially.
- Prioritize working functionality; refine the design afterward.
- Interface language: English only for the first version.

### Identity and data storage

- Optional Google sign-in and recovery are available from the header account icon.
- One person per app installation; no separate profiles.
- Automatically assigned Firebase anonymous user ID is accepted.
- Firebase SQL Connect for cloud storage.
- Offline logging with synchronization when connectivity returns.
- Logging must work on the first launch without internet, before Firebase
  authentication. Save visits locally, establish an anonymous identity when
  connectivity becomes available, and upload those pending visits to that identity.
- Firebase project ID: `counter-f0787`. The owner confirms SQL Connect is enabled.
- SQL Connect service ID: `counter-f0787-service`; location: `europe-central2`.
  The connector ID still needs verification; `counter-f0787-database` was supplied
  tentatively as a database name, not confirmed as a connector ID.
- Today shows “Saved locally · waiting to sync” when changes have not reached
  Firebase. Pending edits and deletions count as unsynchronized changes too.
- Google-linked accounts can restore SQL history after reinstalling or changing
  phones. Old anonymous history can be copied by a project administrator to the
  verified Google-linked UID without modifying the original rows. Existing local
  unsynced changes must survive restore. See `docs/RECOVERY.md` for the workflow.
- A Settings action to delete all local/cloud data is not required for this version.
  Individual visit deletion remains required.

### Initial testing and distribution

- First goal: testing on an Android emulator and the owner's physical phone.
- The owner's phone runs Android 16.
- Minimum supported Android version: Android 16 (API 36); set `minSdk = 36`.

## Initial acceptance criteria

| ID | Behavior | Acceptance criterion |
| --- | --- | --- |
| A1 | Three logging actions | Poop/Pee each save their respective type; Both saves one combined visit. |
| A1a | Quick saving | Tapping a logging button saves immediately without a form or note prompt. |
| A2 | Timestamps | Every visit has a saved date/time visible in history. |
| A3 | Totals | Both contributes one poop and one pee; other types contribute only to their own total. |
| A4 | Calendar days | Daily totals follow midnight-to-midnight days in the current phone time zone; changing zones regroups saved visits without changing their instants. |
| A5 | Past entries | Users can save a forgotten visit with a past date/time. |
| A6 | Validation | Future date/time values are rejected for additions and edits. |
| A7 | Corrections | Editing or deleting updates the affected daily totals and latest-event calculations. |
| A7a | Deletion confirmation | Cancelling leaves the visit unchanged; confirming deletes it. |
| A8 | Notes | A visit can be saved with or without a note; notes can be edited. |
| A8a | Note entry | Users open an entry in Today to add or edit its note. |
| A9 | History | Calendar and daily list support selecting a day and viewing its visits. |
| A9a | History navigation | Today initially selects the current date; Go to today returns to it after browsing past dates. |
| A9b | Calendar totals | Calendar dates display separate poop and pee totals. |
| A9c | Empty days | The daily list displays days with zero entries within its supported date range. |
| A9d | History range | With recorded visits, the daily list covers the earliest visit through today, including empty days. |
| A9e | Empty history | Without any visits, show today, both totals at zero, and “No visits recorded”. |
| A9f | Midnight | A view following today advances at midnight; a selected past-day view stays on that date. |
| A10 | Summary | Today shows the selected date totals, chart, and visits; current-day elapsed indicators use the latest visit across all days. Past dates show the last times on that date. |
| A10a | No previous visit | Show “Not recorded yet” for a type with no recorded visits. |
| A11 | Identity | Anonymous identity setup does not ask the user for login credentials. |
| A12 | Offline | Offline visits remain available locally and synchronize when connectivity returns. |
| A12a | First launch offline | Users can log before Firebase authentication; those entries sync under the assigned anonymous identity once connected. |
| A12b | Pending sync | Today shows “Saved locally · waiting to sync” while saved changes await synchronization. |
| A13 | Android support | The app runs on Android 16 and newer; installation on earlier versions is unsupported. |

These are specification criteria, not completed implementation or test results.

## Confirmed technical foundation

- Native Android application written in Kotlin.
- Jetpack Compose for the UI.
- Room for local visit storage and offline support.
- Firebase SQL Connect for cloud records.
- Firebase Anonymous Authentication for user identity with no login screen.
- Firebase project: `counter-f0787`; SQL Connect enabled as confirmed by the owner.
- Minimum SDK: API 36 (Android 16).

## Scope boundary

The agreed first version covers logging, correction, notes, history, anonymous
identity, and offline/cloud synchronization. Google sign-in and administrator-assisted
recovery were added on 2026-10-25. Full multi-device conflict resolution remains deferred.

## Technical design to resolve

- Application ID and compatible build/dependency versions.
- Verify existing Firebase service/connector configuration, region, Android app
  registration, and whether anonymous authentication is enabled. Enabling SQL
  Connect alone does not establish that the app schema and operations are deployed.
- Local storage and synchronization queue for offline creation, edits, and deletion.
- Stable visit IDs and retry behavior that prevents duplicate uploads.
- Implementation of initial local ownership and linking pending records to the
  Firebase anonymous identity after the first successful connection.
- Server authorization that restricts records to their authenticated owner.
- Synchronization conflicts and pending deletions.
- Timestamp, time zone, visit type, and note schema.
- Sync status and save/upload error handling.
- Backup, data deletion, retention, and privacy behavior.

These are engineering details to settle during implementation, not additional
confirmed product features. The Android Firebase configuration has been verified.

## Implementation milestones

1. **Starter app:** create the Android Studio project, set minimum SDK to 36,
   and run it on an Android 16 emulator and the physical phone.
2. **Local functionality:** implement Room visits, quick logging, manual past
   entries, editing, notes, and confirmed deletion. All actions must work offline.
3. **History:** implement calendar totals, daily list including empty days,
   selected-day visits, global elapsed-time indicators, midnight behavior, and
   current-time-zone regrouping.
4. **Firebase integration:** configure anonymous authentication and owner-scoped
   SQL Connect operations in `counter-f0787`; add pending-change synchronization
   for creation, edits, and deletion, including first-launch offline records.
5. **Validation:** verify the acceptance criteria, fix failures, and produce a
   debug APK for emulator and phone testing. Refine visual design afterward.

## Validation plan

- Unit tests: Poop/Pee/Both totals, elapsed-time calculations, empty states,
  history range, midnight transitions, and time zone changes across date boundaries.
- Date/time tests: reject future manual timestamps and handle daylight-saving
  gaps and repeated local times explicitly when converting manual input to instants.
- Room tests: persistence across reopening, corrections, and pending changes.
- UI tests: three quick-log buttons, past-entry form, notes, deletion confirmation,
  calendar/list navigation, Today button, and pending-sync message.
- Firebase integration tests: users cannot read or modify another user's records;
  offline inserts, edits, and deletions synchronize without duplicates or resurrection.
- Manual checks: first launch in airplane mode, reconnecting, closing/reopening
  with pending changes, rapid taps, rotation, midnight, and changing time zones.
- Verify on both an Android 16 emulator and the owner's Android 16 phone.
- Completion requires all agreed acceptance criteria to pass. See the implementation
  checkpoint below for checks already completed and outstanding validation.

## Android Studio starting point

Finish installing Android Studio and SDK tools. The first milestone is an
Empty Activity project using Kotlin/Jetpack Compose running on a phone or emulator.
Set the app name to **counter** and minimum SDK to **API 36 (Android 16)**.
Use the checkout at `C:\Projects\counter`, preserving `.git` and this specification.
Review Android ignore rules before committing generated files. The current `*.jar`
rule needs an exception for the generated Gradle wrapper JAR.

## Implementation checkpoint — 2026-10-10

- 2026-10-11 visual revision: follow the newly supplied learning-app screens with
  deep blue backgrounds, slate blue cards, warm gold accents, rounded navigation,
  and matching poop, droplet, calendar, and action icons. Use the same palette in
  both system appearance modes and a gold background behind the poop launcher icon.

- Visual direction updated at the owner's request using the supplied Yatre image:
  blue headers, navy quick-log buttons/navigation, white rounded History cards,
  simple line icons, and a matching dark palette. Functional requirements are retained.

- Implemented local Room storage, all three logging actions, manual entries,
  editing, notes, confirmed deletion, and the History calendar/list/summary.
- Implemented current-time-zone grouping and following today at midnight.
- Pending changes and deletion markers upload using WorkManager with network
  constraints and retry/backoff. Anonymous identity is bound to this installation.
- Firebase Android config, authentication, and SQL Connect client code are present.
  Deployed owner-scoped schema/operations for the `counter` connector in europe-central2.
  Cloud SQL instance `counter-f0787-instance` and database `counter-f0787-database`
  were verified through the CLI. Authentication was enabled by the owner.
- Local SQL Connect emulator tests pass for anonymous access, owner isolation,
  deletion, stale/duplicate retries, and rejection of unauthenticated reads.
- Live Android SDK integration test passes with disposable anonymous users for
  uploads, edits, deletion, owner isolation, and delayed/duplicate revisions.
- First-launch offline logging and upload after reconnection were checked on the
  API 37 emulator: a Both visit remained pending offline and the background worker
  completed successfully after reconnection, clearing the pending indicator.
- Debug APK builds. Ten domain unit tests, four sync tests, and the sample test pass.
  Six instrumented tests pass on the available API 37 emulator, covering Room
  persistence and corrections, quick logging, confirmation, calendar width, and Firebase.
- Lint completes with no errors using an opt-in workaround that skips only the
  stalled BidiSpoofing detector. Twenty-five warnings remain, mainly template resources
  and dependency-update notices. A source scan found no bidirectional controls.
- Following the visual refresh, UI/Room instrumented tests passed on the connected
  Android 16 Samsung phone and API 37 emulator. Full manual lifecycle/accessibility
  and phone acceptance checks remain pending.

## References

- [Compose setup](https://developer.android.com/develop/ui/compose/setup)
- [Android 16 SDK setup](https://developer.android.com/about/versions/16/setup-sdk)
- [Room](https://developer.android.com/training/data-storage/room)
- [Firebase SQL Connect API](https://firebase.google.com/docs/reference/sql-connect/rest)
- [Firebase anonymous authentication](https://firebase.google.com/docs/auth/android/anonymous-auth)

References inform technical proposals; they do not settle open product decisions.

