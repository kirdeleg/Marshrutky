# Marshrutky

An Android app that answers one question: when does the next minibus leave?

It covers the suburban routes around Merefa and Kharkiv, where timetables exist mostly as
photographed paper tables and posts in local groups. The app shows the next departures for the
routes you care about, counts down to each one, and works offline once it has the data.

The interface is in Ukrainian only.

## What it does

- **Favourites** — a card per route with the next three departures in each direction and a
  countdown to the closest one. Cards can be dragged into the order you want.
- **Nearest** — pick a boarding stop and see every upcoming departure from it, across all routes.
  Useful when several routes pass the same stop and only some of them suit you.
- **Routes** — the full list, searchable by number or name, with the complete timetable for
  today, weekdays, Saturday and Sunday.
- **Settings** — theme (light, dark, system) and a manual refresh of the schedules.

Departures that have already left are dimmed rather than hidden, and once the last one for the day
has gone the direction says so instead of quietly showing tomorrow.

## Where the schedules live

There are no timetables inside the APK. The app reads the [`routes/`](routes) directory of this
repository through the GitHub Contents API, downloads the JSON files and keeps them on the device.

That means:

- publishing a new route is a commit, not a release;
- the app needs network access **once**, on first launch, and works offline afterwards;
- a refresh only downloads files whose blob SHA changed, so an unchanged set costs a single
  request;
- refreshes happen at most every six hours, and only when the device is actually online. The
  button in Settings ignores the interval.

## Schedule format

One file per route in `routes/`. The file name is the route identifier, so **renaming a file makes
the app treat it as a different route** and drops it from anyone's favourites.

A route that stops at several places along the way lists each of them with its own times:

```json
{
  "number": "1665",
  "name": "Островерхівка — Харків",
  "directions": [
    {
      "label": "На Харків",
      "destination": "Харків",
      "stops": [
        {
          "name": "Островерхівка",
          "schedule": {
            "weekday": ["05:50", "09:30", "14:30", "17:30"],
            "saturday": ["05:50", "09:30", "14:30", "17:30"],
            "sunday": ["05:50", "14:30"]
          }
        },
        {
          "name": "Мерефа (Селекційна)",
          "schedule": {
            "weekday": ["06:20", "10:00", "15:00", "18:00"],
            "saturday": ["06:20", "10:00", "15:00", "18:00"],
            "sunday": ["06:20", "15:00"]
          }
        }
      ]
    }
  ]
}
```

| Field | Required | Notes |
| --- | --- | --- |
| `number` | no | Shown in a badge next to the name. Suburban routes are often unnumbered. |
| `name` | yes | Endpoints, e.g. `Островерхівка — Харків`. |
| `directions` | no | Usually two: there and back. |
| `directions[].label` | yes | Never displayed; kept only as a fallback stop name for old files. |
| `directions[].destination` | no² | Where this direction ends. The Nearest tab shows it instead of the route name, because at a stop the name says nothing about which way the bus is going. |
| `directions[].stops` | yes¹ | Boarding points in travel order, each with its own times. |
| `stops[].name` | yes | Stops with the same name in different files are treated as one place, which is what makes the Nearest tab work. |
| `stops[].schedule` | yes | Three lists: `weekday`, `saturday`, `sunday`. An empty list means the route does not run that day. |

Times are `HH:MM`. Unparseable entries are dropped, duplicates removed, and the rest sorted, so the
order inside a list does not matter. A file that fails to parse is skipped without affecting the
other routes.

¹ Files written before multi-stop support are still read: a direction may carry a single
`boardingStop` string and a `schedule` object directly, instead of a `stops` list.

² It cannot be derived from the other fields, which is why it is stored: the terminus is not a
boarding point, and the settlement in the name may differ from the one in the stop — route 1624 is
`Зелений Гай — Харків` but departs from `Високий (Зелений Гай)`. Omitting it is safe: the Nearest tab
then falls back to the route name.

## Adding or fixing a route

Open a pull request that adds or edits a file in `routes/`. Nothing else needs to change — the app
picks the file up on its next refresh. Accuracy matters more than coverage here: a wrong departure
time is worse than a missing route, because it sends someone to an empty stop.

## Building

Requires JDK 17 or newer and the Android SDK. `minSdk` is 26, `targetSdk` is 36.

```sh
./gradlew :app:assembleDebug        # debug build
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintRelease          # lint, NewApi is fatal
```

The release build type is signed with the debug keystore so it can be installed over a debug build
during testing. A real key would be needed to publish anywhere.

## How it is put together

Kotlin, Jetpack Compose with Material 3 Expressive, Koin for dependency injection, DataStore for
preferences, kotlinx.serialization for the JSON, Navigation 3 for the back stack.

```
app/src/main/java/com/kdelehoi/marshrutky/
├── data/          cache on disk, GitHub client, preferences
├── domain/        route model and departure calculations
├── ui/            screens, shared components, theme
└── viewmodel/     screen state
```

The app is built to do nothing while you are not looking at it. The clock ticks once a minute
rather than once a second, it is a cold flow that only runs while a screen showing times is
visible, and every piece of state is shared with `WhileSubscribed`, so minimising the app stops the
whole chain instead of leaving it running behind a promise not to.

## Limitations

- The data source is this repository, hard-coded. Other regions would need their own build.
- Intermediate stop times exist only for routes where that data was available.
- No widget, no notifications, no live vehicle positions — the timetables are all there is.
