# StrideSet: Stage 3 Report (Build)

## 1. Development environment, technology stack and architecture

| Area | Choice |
|---|---|
| IDE / build | Android Studio, Gradle, AGP 9.5, version catalog `gradle/libs.versions.toml` |
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Platform | Android only, `minSdk 26`, `targetSdk`/`compileSdk 37`, package `ee.ut.cs.mad.strideset` |
| Persistence | Room 2.8 (SQLite) for app data, DataStore Preferences for unit settings |
| Testing | JUnit 4 (JVM), AndroidX Test + Compose UI Test + Room Testing (device/emulator) |
| Collaboration | GitHub, feature branches, PRs with approval based system, GitHub Issues |
| CI | GitHub Actions (`.github/workflows/ci.yml`) builds `assembleDebug` on every PR into `main` |

**Architecture.** A single-activity app with layered data access, following the Android Basics with Compose pattern:

```
Room DAO / DataStore  →  Repository (interface + Room implementation, exposes Flow)
   →  AppContainer (manual dependency injection, created in StrideSetApplication)
   →  MainActivity (collects Flows, turns errors into messages)
   →  StrideSetApp (navigation + screen state)  →  stateless Compose screens
```

- **Packages:** `data/` (models, repositories, Room), `running/` (GPS tracking and foreground service), `ui/` (screens, `components/`, `calendar/`, `running/`, `theme/`).
- **Domain models** (`Workout`, `Run`, `WorkoutPlan`, `Exercise`) are separate from Room entities, so screens don't depend on database details.
- **State:** most screens are stateless composables fed by `StrideSetApp`.

## 2. Frontend screens and navigation

| Screen | Purpose |
|---|---|
| Home | Start a workout or a run |
| Choose workout | Pick a saved plan, start an empty workout, edit plans |
| Plan editor | Create/edit a plan: exercises with per-set reps and weight |
| Workout | Log sets (reps, weight, completed), add exercises/sets, finish or discard |
| Workout complete | Summary (duration, exercises, sets), "View calendar" or "Done" |
| Run | GPS run tracking: distance, active time, pace, pause/resume/finish/discard |
| Calendar | Month grid with a dot on active days, and the selected day's workouts and runs |
| Settings | Weight unit (kg/lb), distance unit (km/mi) |

- **Navigation:** an `enum AppDestination` in `ui/StrideSetApp.kt`, stored with `rememberSaveable` so it survives rotation. There's a bottom bar with three tabs (Home, Calendar, Settings). Workout, plan and run flows open on the Home Screen.
- **Back:** each screen has a fixed parent (Workout/Plan editor → Choose workout, others → Home). Android Back is handled with `BackHandler`. During an active workout or run, Back does not silently leave: the run screen asks whether to keep tracking in the background, finish or discard. Navigation is covered by `AppNavigationTest`.
- **Design:** implemented from the team's Figma design system, using shared theme tokens (`Color`, `Type`, `Dimens`) and shared components (`ScreenLayout`, `PrimaryButton`, `SecondaryButton`, `RoundIconButton`, `UnitSelector`).

## 3. Backend/API system and endpoints

**Not applicable.** StrideSet is an offline, single-user app by design. There is no server or REST API.

The app's internal "API" is a set of repository interfaces in `data/repository`:

| Repository | Main operations |
|---|---|
| `ExerciseRepository` | observe exercises, add custom exercise (duplicate names rejected) |
| `WorkoutPlanRepository` | observe, save, delete plans |
| `WorkoutRepository` | observe active workout, start, add exercise/set, update/delete set, finish, discard, finished workouts by date range, last performance per exercise |
| `RunRepository` | observe active run, start, save progress, pause, finish, discard, finished runs by date range |
| `SettingsRepository` | observe and change units (DataStore) |

## 4. Database design and data flow

There's one Room database (`strideset.db`). Every version's schema is exported to `app/schemas/`, and every upgrade is covered by migration tests (1→2 and 3→5 are auto-migrations, 2→3 is manual).

| Table | Content | Relations |
|---|---|---|
| `exercises` | built-in and custom exercises (name, type, `isCustom`) | — |
| `workout_plans` | plan name | — |
| `plan_exercises` | exercises in a plan, ordered | → plan (cascade delete), → exercise (restrict) |
| `plan_sets` | target reps and weight per set | → plan exercise (cascade) |
| `workouts` | start/finish time, name | → plan (set null if the plan is deleted) |
| `workout_exercises` | exercises in a workout, ordered | → workout (cascade), → exercise (restrict) |
| `workout_sets` | reps, `weightKg`, completed | → workout exercise (cascade) |
| `runs` | start/finish time, active duration, `distanceMeters`, paused | — |

- **Units:** values are always stored in metric (`weightKg`, `distanceMeters`) and converted only for display or input. Changing the unit therefore converts all existing data correctly.
- **Sessions are persisted, not held in memory:** an active workout or run is a row with `finishedAt = null`, and each logged set or tracking update is written immediately. A session survives the app being backgrounded or killed. Only one active session of each kind exists at a time.
- **Seed data:** built-in exercises and sample plans are inserted when the database is first created.
- **Data flow:** DAOs return Kotlin `Flow`s. Repositories map entities to domain models, and the UI collects them with `collectAsState`, so screens update automatically when data changes. For example, a finished workout appears on the Calendar right away. Writes are `suspend` functions called from coroutines.
- **Run tracking:** platform GPS (`LocationManager`) → `TrackingEngine` (distance and pace, filtering bad points) → `RunSession` → `RunRepository`, running in a location **foreground service** with a notification.

## 5. Authentication, permissions and security decisions

- **Authentication:** none, by design. The app is single-user and offline, with no accounts and no server data.
- **Permissions (minimal, all for running only):**

  | Permission | Why |
  |---|---|
  | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS distance tracking |
  | `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | keep tracking while the screen is off or another app is open |
  | `POST_NOTIFICATIONS` | ongoing "Run in progress" notification |
  | `WAKE_LOCK` | keep tracking alive during a run |

  Location is requested at runtime only when a run starts. If precise location is denied, the app explains why it's needed and links to settings. Gym features work without any permission.
- **Security:** there's no network access, no API keys and no secrets in the repository. `local.properties`, keystores (`*.jks`, `*.keystore`) and `google-services.json` are git-ignored. All data stays on the device. The run tracking service is not exported. `main` is protected through PR review.

## 6. Third-party integrations and optional admin tools

- **Third-party integrations:** none beyond Google's AndroidX/Jetpack libraries. Location uses Android's built-in `LocationManager` rather than Google Play Services, so the app has no Google account or Play Services dependency.
- **Admin tools:** not applicable. There's no server-side content or other users to manage.

## 7. Staging and production environment setup

| Environment | Setup |
|---|---|
| Development / staging | `debug` build, run on emulators and team members' physical phones. Instrumented tests run against it (`connectedDebugAndroidTest`) |
| Production | `release` build type exists (code optimization currently disabled). A signing configuration is not yet set up |
| CI | GitHub Actions builds `assembleDebug` on every PR. Unit tests and lint (`testDebugUnitTest lintDebug`) are run locally before review |

There's no separate staging backend, since the app has no backend. Separating debug and release on one device (`applicationIdSuffix ".debug"`) and release signing are remaining work.

## 8. Loading, offline and error-handling strategies

- **Offline:** the whole app works offline, and there is no online mode to fall back from.
- **Loading:** data is local and loads almost instantly. A progress indicator is shown while the active workout or a run is being loaded. Lists start empty and fill when the first database result arrives.
- **Empty states:** "Add your first exercise…" for an empty workout, "No activities on this day" on the Calendar, and a grid without dots when there's no history.
- **Errors:**
  - Failed workout/plan saves, starts and finishes show a message, and the data stays as it was.
  - Finishing a workout without a completed set is refused with an explanation.
  - Duplicate custom exercise names and invalid plan input are rejected with a message.
  - The run screen has dedicated error states with **Retry**: load failure, GPS off, precise location denied, and failing to save progress.
  - Settings load/save failures show a message.
- **Data safety:** sessions are written as they happen. An interrupted run is restored with its saved progress. Discarding a workout or run asks for confirmation first.

## 9. Challenges, solutions, limitations and remaining work

**Challenges and solutions**

| Challenge | Solution |
|---|---|
| Team new to Kotlin/Android, 5 people editing one navigation file | Simple enum navigation, shared components and tokens, small PRs with mandatory review, merging `main` into feature branches regularly |
| Never losing an in-progress workout/run (app killed, phone rotated) | Sessions stored in the database immediately (`finishedAt = null`), `rememberSaveable` for UI state |
| Reliable GPS distance | Separate, unit-tested `TrackingEngine` that filters inaccurate points; foreground service for background tracking |
| Changing the schema while teammates had data | Exported schemas, auto/manual migrations, migration tests |
| Calendar month layout (4–6 weeks, Monday first) | Pure Kotlin `calendarGridDays` written test-first. The grid only shows the weeks a month needs, matching Figma |

**Test coverage:** about 37 JVM unit tests (tracking, run session, formatting, units, calendar grid, settings) and about 70 instrumented tests (Room repositories and migrations, navigation, workout, run and plan screens, run service).

**Known limitations**
- Some bugs remain in the newly merged workout and run flows. These are planned for the testing/launch phase.
- **Progressive overload:** the "last performance" data and a UI slot exist, but the suggestion is not yet connected to the workout screen.
- Home does not yet offer "Resume" for an active session.
- Calendar: activity rows open no detail view; workouts and runs on the same day are not sorted by time; a database read error isn't shown as a message.
- ViewModel is only used for runs. The other screens get their data through `StrideSetApp`.
- CI only builds the app; tests and lint are run manually.
- Visual polish against Figma is incomplete (spacing, icons, a few text styles).

**Remaining work (testing and launch phase)**
1. Fix the known workout/run bugs and test all scenarios.
2. Connect the progressive-overload suggestion, and add "Resume" on Home.
3. Add tests and lint to CI. Set up release signing and a separate debug application ID.
4. Accessibility and design polish pass, then update Figma to match the decisions made during the build.
5. User guide and developer guide in the README.

## AI use disclaimer

All team members used AI tools during this stage. Each person chose their own tools and way of working.

**Rules that applied to everyone, whatever the tool:**
- Every code change reached `main` only through a GitHub pull request with at least one approving review from another team member. AI never approved or merged anything.
- Whoever opened a PR was responsible for understanding and testing its code, including AI-written parts.

The design decisions were made by the team, and all AI-written code was reviewed and tested on a phone or an emulator before its PR. The team is ultimately responsible for all final code in this submission.

This report was drafted with AI assistance from the repository's code and documentation, then checked and edited by the team.
