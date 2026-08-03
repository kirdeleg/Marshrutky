#!/usr/bin/env python3
"""Розкладає зведений папірець «Мерефа — Харків» на окремі файли маршрутів.

Кожен рядок папірця — це обіг машини: виїзд із початкового пункту, дві зупинки в Мерефі
і виїзд назад із Холодної Гори. Усі чотири колонки підписані «відправлення», тож остання —
це не прибуття до Харкова, а час, коли машина рушає звідти назад.

Дорога на Харків описана трьома точками посадки, тож той, хто сідає в Мерефі на транзитний
рейс, бачить свій час, а не час виїзду з далекого села. Для зворотного напрямку такої
деталізації в папірці немає — там відома лише Холодна Гора.
"""

import json
import pathlib
import re

ROUTES = pathlib.Path(__file__).resolve().parent.parent / "routes"

DAILY = ("weekday", "saturday", "sunday")
NO_SUNDAY = ("weekday", "saturday")
WORKDAYS = ("weekday",)

SELEKTSIINA = "Мерефа (Селекційна)"
SMACHNI = "Мерефа (Смачні історії)"
KHARKIV = "Харків (Холодна Гора)"

# (маршрут, початковий пункт, Селекційна, Смачні історії, Холодна Гора, дні)
TABLE = [
    ("Яковлівка", "05:30", None, None, "06:35", WORKDAYS),
    ("Рокитне", "05:20", "05:40", "05:50", "06:45", NO_SUNDAY),
    ("Утківка", "05:50", None, "06:00", "07:00", NO_SUNDAY),
    ("1609", None, "06:10", "06:20", "07:30", DAILY),
    ("Соколове", None, None, None, "07:45", DAILY),
    ("Островерхівка", "05:50", "06:20", "06:30", "08:00", DAILY),
    ("Соколове", "06:00", "06:30", "06:40", None, DAILY),
    ("Липкуватівка", "06:15", "06:40", "06:50", None, DAILY),
    ("Нова Водолага", "06:20", "06:50", "07:00", None, NO_SUNDAY),
    ("Безпалівка 1", "06:25", "07:00", "07:10", "08:30", DAILY),
    ("1609", None, "07:30", "07:40", None, NO_SUNDAY),
    ("Утківка", "07:50", None, "08:00", "08:50", NO_SUNDAY),
    ("Липкуватівка", None, None, None, "09:00", DAILY),
    ("Яковлівка", "07:40", None, "08:10", "09:10", WORKDAYS),
    ("1609", None, "08:20", "08:30", "09:30", NO_SUNDAY),
    ("Рокитне", None, None, None, "09:50", NO_SUNDAY),
    ("Безпалівка 2", "08:15", "08:50", "09:00", "10:10", DAILY),
    ("Нова Водолага", None, None, None, "10:30", NO_SUNDAY),
    ("Соколове", "09:00", "09:30", "09:40", None, DAILY),
    ("Островерхівка", "09:30", "10:00", "10:10", "11:00", NO_SUNDAY),
    ("Утківка", "10:25", None, "10:40", None, NO_SUNDAY),
    ("Соколове", None, None, None, "11:30", DAILY),
    ("Безпалівка 1", "10:25", "11:00", "11:10", "12:00", DAILY),
    ("Утківка", None, None, None, "12:30", NO_SUNDAY),
    ("Липкуватівка", "11:10", "11:35", "11:45", "13:00", NO_SUNDAY),
    ("Безпалівка 2", "11:35", "12:10", "12:20", "13:30", DAILY),
    ("1609", None, "12:40", "12:50", "14:00", DAILY),
    ("Нова Водолага", "12:40", "13:10", "13:20", "14:30", NO_SUNDAY),
    ("Рокитне", "13:10", "13:40", "13:50", None, NO_SUNDAY),
    ("1609", None, "14:00", "14:10", "15:00", DAILY),
    ("Рокитне", None, None, None, "15:15", NO_SUNDAY),
    ("Безпалівка 1", "13:55", "14:30", "14:40", "15:40", DAILY),
    ("Соколове", "14:10", "14:40", "14:55", "15:55", NO_SUNDAY),
    ("Островерхівка", "14:30", "15:00", "15:10", "16:05", DAILY),
    ("1609", None, "15:30", "15:40", "16:40", DAILY),
    ("Безпалівка 2", "15:10", "15:45", "15:55", None, DAILY),
    ("Липкуватівка", None, "16:00", "16:10", "17:00", DAILY),
    ("Безпалівка 2", None, None, None, "17:30", DAILY),
    ("Нова Водолага", "16:00", "16:40", "16:50", "17:45", NO_SUNDAY),
    ("Утківка", "16:50", None, "17:10", "18:00", NO_SUNDAY),
    ("Рокитне", "16:45", "17:10", "17:20", "18:10", NO_SUNDAY),
    ("1609", None, "17:40", "17:50", "18:40", DAILY),
    # Рядок «Соколове, Островерхівка 17:00/17:30» — одна машина через обидва села. Мереф'янські
    # колонки віддані Островерхівці: якби вони стояли в обох файлах, у списку рейсів від
    # Селекційної той самий автобус о 18:00 з'явився б двічі.
    ("Соколове", "17:00", None, None, "19:10", NO_SUNDAY),
    ("Островерхівка", "17:30", "18:00", "18:10", "19:10", NO_SUNDAY),
    ("1609", None, "18:20", "18:30", "19:30", NO_SUNDAY),
    ("1609", None, "19:10", "19:20", "20:20", DAILY),
]

# маршрут -> (ім'я файлу, номер, назва початкової зупинки)
# 1609 стартує з самої Мерефи, тож власного початкового пункту в папірці не має.
ROUTE_INFO = {
    "1609": ("1609-merefa-kharkiv", "1609", None),
    "Безпалівка 1": ("bezpalivka-1-kharkiv", None, "Безпалівка 1"),
    "Безпалівка 2": ("bezpalivka-2-kharkiv", None, "Безпалівка 2"),
    "Липкуватівка": ("lypkuvativka-kharkiv", None, "Липкуватівка"),
    "Нова Водолага": ("nova-vodolaha-kharkiv", None, "Нова Водолага"),
    "Островерхівка": ("1665-ostroverkhivka-kharkiv", "1665", "Островерхівка"),
    "Рокитне": ("1627-rokytne-kharkiv", "1627", "Рокитне"),
    "Соколове": ("1660-sokolove-kharkiv", "1660", "Соколове"),
    "Утківка": ("1625-utkivka-kharkiv", "1625", "Утківка"),
    "Яковлівка": ("yakovlivka-kharkiv", None, "Яковлівка"),
}

ROUTE_NAMES = {"1609": "Мерефа — Харків"}


def empty_week():
    return {day: [] for day in DAILY}


def add(week, time, days):
    if time:
        for day in days:
            week[day].append(time)


def stop(name, week):
    return {"name": name, "schedule": {day: sorted(times) for day, times in week.items()}}


def has_times(week):
    return any(week[day] for day in DAILY)


def write(file_name, number, name, directions):
    payload = {"name": name, "directions": directions}
    if number:
        payload = {"number": number, **payload}
    text = json.dumps(payload, ensure_ascii=False, indent=2)
    # Списки часу тримаємо в один рядок, як у файлах, заповнених руками.
    text = re.sub(r"\[[\s\n]*((?:\"\d\d:\d\d\",?\s*)+)\]", lambda m: "[" + " ".join(m.group(1).split()) + "]", text)
    (ROUTES / f"{file_name}.json").write_text(text + "\n", encoding="utf-8")


def main():
    columns = {route: {"origin": empty_week(), SELEKTSIINA: empty_week(),
                       SMACHNI: empty_week(), KHARKIV: empty_week()}
               for route in ROUTE_INFO}

    for route, origin, selektsiina, smachni, kharkiv, days in TABLE:
        week = columns[route]
        add(week["origin"], origin, days)
        add(week[SELEKTSIINA], selektsiina, days)
        add(week[SMACHNI], smachni, days)
        add(week[KHARKIV], kharkiv, days)

    for route, (file_name, number, origin_name) in sorted(ROUTE_INFO.items()):
        week = columns[route]

        outbound = []
        if origin_name and has_times(week["origin"]):
            outbound.append(stop(origin_name, week["origin"]))
        for name in (SELEKTSIINA, SMACHNI):
            if has_times(week[name]):
                outbound.append(stop(name, week[name]))

        write(
            file_name,
            number,
            ROUTE_NAMES.get(route, f"{route} — Харків"),
            [
                {"label": "На Харків", "stops": outbound},
                {"label": "З Харкова", "stops": [stop(KHARKIV, week[KHARKIV])]},
            ],
        )

    print(f"Записано {len(ROUTE_INFO)} маршрутів")


if __name__ == "__main__":
    main()
