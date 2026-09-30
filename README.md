# Guddiz

A private, offline-first Android business notebook for a Pakistani cosmetics shop. Built with Kotlin, Jetpack Compose, Room, and MVVM.

Fully offline. No server. No login. No cloud.

## Modules

- **Dashboard** — 7-day sales chart, stock alerts, live totals
- **Products** — photos, pricing, stock, expiry, movement history
- **Sales** — multi-product sales across 5 channels with profit
- **Purchases** — supplier orders with auto-computed costs
- **Expenses** — 13 categories including COD and Personal Use
- **PR / Marketing** — multi-product packages with status
- **Gifts** — multi-product gifts with occasion
- **FOC** — free-of-cost records
- **Records** — unified search across all modules
- **Reports** — period filters, top products, category breakdown
- **Backup** — single ZIP with database + receipts
- **Recycle Bin** — 30-day soft delete with restore

## Architecture

MVVM with a Repository layer. Compose for UI, Room for persistence, Kotlin Flows for reactive state.
UI (Compose)
|
ViewModel
|
Repository
|
Room + SQLite

text

Each layer only talks to the one below it. Never skip. Never go backward.

## Database schema
products --+--> purchases
+--> sale_items --> sales
+--> pr_package_items --> pr_packages
+--> gift_items --> gifts
+--> foc_records
+--> stock_movements (audit log)

text

6 versions of migrations (v1 to v6). No destructive fallback.

## Tech stack

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Room 2.6.1
- Coroutines + Flow
- Navigation Compose
- Coil
- Manual DI via AppContainer

## Key design decisions

1. Money is a value class backed by Long paisa. No Float/Double rounding errors.
2. Soft delete via deletedAt column. 30-day recycle bin.
3. CostCalculator is the only place money math happens.
4. Flow for all reads. Zero manual refresh calls.
5. Explicit Room migrations only.
6. Stock movement audit log for every change.
7. Backup ZIP includes the database and all receipt images.

## Testing

79 unit tests, all passing.
./gradlew test

text

## Build
./gradlew assembleDebug # Debug APK
./gradlew assembleRelease # Signed release APK
./gradlew test # Run unit tests

text

## License

MIT
