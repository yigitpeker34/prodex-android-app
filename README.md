# ProdEx – Productivity & Experience (Android)

ProdEx is an all-in-one life companion app that combines tasks, expenses, a relaxation game and local events in one place, to reduce the effort of switching between separate apps. Built for the **Mobile Computing** course of the M.Sc. Professional IT Business & Digitalization at **HTW Berlin** (Winter Semester 2025/26).

## Features

| Module | What it does |
|---|---|
| Account and profile | Sign up and sign in with email and password, password rules (at least 8 characters with upper case, lower case, digit and special character), edit profile, log out, delete account |
| Tasks | Create, edit and delete tasks; completing a task earns virtual coins |
| Expenses | Log spending by category, pie chart and list of transactions, filters by period |
| Game Hub | Berlin Runner game that is free to play at any time; earned coins can unlock an extra character |
| Event Finder | Shows events near the user, using the device location and the Ticketmaster Discovery API |

## Security and privacy

- Passwords are never stored in plain text: each password is combined with a random per-user salt and hashed with SHA-256 (see `util/PasswordHasher.kt`).
- Answers to security questions are stored hashed as well.
- Tasks, expenses and the coin wallet are stored locally on the device in a Room database.
- Location is only requested for the Event Finder.

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM with a ViewModelFactory for manual dependency injection |
| Local database | Room (SQLite) |
| Networking | Retrofit and OkHttp, JSON parsing |
| Build | Gradle (Kotlin DSL) |

## Setup

1. Clone the repository and open it in Android Studio.
2. Get a free API key at https://developer.ticketmaster.com and replace the placeholder `TM_API_KEY` in `app/src/main/java/com/yigit/prodex/ui/events/EventFinderScreen.kt`.
3. Run the app on an emulator or a device.

## Author

Yigit Peker, HTW Berlin
