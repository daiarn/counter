# Counter — Android app specification

Status: Implemented for emulator and phone testing; remaining validation is tracked below.
Updated: 2026-11-04
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
- Use the established blue-and-gold appearance described below.

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
- Place a weekly trends card above the calendar, with Monday–Sunday daily counts.
- Start with the current week; show its date range and previous/next-week controls.
  Disable next week when viewing the current week. Follow week rollover while
  viewing the current week; deliberately selected past weeks remain selected.
- Draw separate gold poop and dashed pale-blue pee lines, with distinct point markers.
  Display color-matched counts beside each day's points, keeping equal counts readable.
- Fit all seven days within the card without horizontal scrolling or a left-side scale.
- Show separate weekly poop/pee totals. Both contributes to each category; deleted
  visits are excluded. Use the phone's current time zone and refresh when visits change.
- Past and current days without recorded visits show zero. Future days have no points
  or count labels and cannot be selected. Label today as ongoing.
- Tapping a chart day, including its weekday/date labels, opens that day in Today.
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
- Use deep blue backgrounds, slate blue cards, warm gold accents, rounded navigation,
  and consistent outline icons, following the supplied learning-app references.
- Keep the same blue palette in system light and dark modes, independent of wallpaper
  colors. Use a gold background for the poop launcher icon.
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
  Deployed connector: `counter`; verified database: `counter-f0787-database`;
  Cloud SQL instance: `counter-f0787-instance`.
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

## Acceptance criteria

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
| A9g | Weekly trends | History shows separate daily poop/pee lines above the calendar for a Monday–Sunday week, with a date range and weekly totals. |
| A9h | Week navigation | Start with this week; browse earlier weeks and return using next week. Future weeks are blocked; the current week follows Monday rollover. |
| A9i | Daily chart labels | All seven days fit without horizontal scrolling or a left scale; color-matched counts appear beside points, including when the two counts are equal. |
| A9j | Partial and empty weeks | Today is marked ongoing; future days are blank and disabled. Recorded-day gaps show zero and an empty week has a no-visits message. |
| A9k | Chart selection and updates | Tapping a past/current chart day or its labels opens Today for that date. Totals use the current phone time zone, count Both in each category, exclude deleted visits, and refresh after changes. |
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
recovery were added on 2026-10-25. Records and weekly trends are also included;
the weekly chart layout was refined on 2026-11-04. Full multi-device conflict
resolution remains deferred.

## Implemented technical decisions

- Application ID: `com.daiarn.counter`; build and dependency versions are maintained
  in the Gradle configuration.
- Room stores visits locally; WorkManager uploads pending creations, edits, and
  deletion markers with network constraints and retry/backoff.
- Stable visit IDs and revision checks protect against duplicate/stale uploads and
  preserve corrections made while an upload is running.
- Anonymous authentication assigns the installation's identity. First-launch offline
  visits remain local until authentication and upload can complete.
- Deployed SQL Connect operations restrict access to the authenticated owner.
- Visit timestamps preserve instants; display and grouping use the current phone zone.
- Android backup/device transfer excludes the visits database and preferences;
  Google-linked cloud restore provides recovery. See `docs/RECOVERY.md`.
- Full multi-device conflict resolution remains deferred. See README for deployment,
  build, and verification instructions.

## Original implementation milestones (historical)

The starter app, local storage, history, and Firebase integration are implemented.
The list below records the original sequence; current validation status follows.

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
- Weekly chart checks: year boundaries, current-week rollover, future-day exclusion,
  weekly totals, live count updates, chart-to-Today navigation, readable daily labels,
  and all seven days fitting without horizontal scrolling.
- Firebase integration tests: users cannot read or modify another user's records;
  offline inserts, edits, and deletions synchronize without duplicates or resurrection.
- Manual checks: first launch in airplane mode, reconnecting, closing/reopening
  with pending changes, rapid taps, rotation, midnight, and changing time zones.
- Verify on both an Android 16 emulator and the owner's Android 16 phone.
- Completion requires all agreed acceptance criteria to pass. See the implementation
  checkpoint below for checks already completed and outstanding validation.

## Development setup

Open the existing project at `C:\Projects\counter` in Android Studio and use an
Android 16 or newer device/emulator. The project and Gradle wrapper are already
configured. Follow README for build commands, tests, and the debug APK location.

## Latest implementation checkpoint — 2026-11-04

- Weekly trends are implemented above the History calendar, including week navigation,
  weekly totals, future-day handling, and tapping days to open Today.
- On November 2, the build, 27 unit tests, and five emulator UI tests passed.
  The UI tests covered weekly boundaries/rollover and chart-to-Today navigation.
  Lint passed with the documented BidiSpoofing workaround; 33 warnings remained.
- On November 4, the left scale and horizontal scrolling were removed and daily
  counts added beside the points. The updated debug build passed and was installed
  on the owner's Samsung phone with existing app data preserved. The owner confirmed
  the revised layout was better. Automated UI tests were not rerun for this layout edit.
- Full manual lifecycle/accessibility and remaining phone acceptance checks remain
  pending. Earlier integration results below are historical, not new test runs.

## Earlier implementation checkpoint — October 2026 (historical)

- 2026-10-11 visual revision: follow the newly supplied learning-app screens with
  deep blue backgrounds, slate blue cards, warm gold accents, rounded navigation,
  and matching poop, droplet, calendar, and action icons. Use the same palette in
  both system appearance modes and a gold background behind the poop launcher icon.

- Earlier visual direction, superseded by the October 11 revision above, used the supplied Yatre image:
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

