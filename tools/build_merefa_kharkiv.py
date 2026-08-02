#!/usr/bin/env python3
"""Розкладає зведений папірець «Мерефа — Харків» на окремі файли маршрутів.

Кожен рядок папірця — це обіг машини: виїзд із початкового пункту, дві зупинки
в Мерефі і виїзд назад із Холодної Гори. Колонки Мерефи тут не використовуються,
бо час посадки беремо з початкового пункту.
"""

import json
import pathlib
import re

ROUTES = pathlib.Path(__file__).resolve().parent.parent / "routes"

DAILY = ("weekday", "saturday", "sunday")
NO_SUNDAY = ("weekday", "saturday")
WORKDAYS = ("weekday",)

# (маршрут, виїзд із початкового пункту, виїзд із Холодної Гори, дні)
TABLE = [
    ("Яковлівка", "05:30", "06:35", WORKDAYS),
    ("Рокитне", "05:20", "06:45", NO_SUNDAY),
    ("Утківка", "05:50", "07:00", NO_SUNDAY),
    ("Соколово", None, "07:45", DAILY),
    ("Островерхівка", "05:50", "08:00", DAILY),
    ("Соколово", "06:00", None, DAILY),
    ("Липкуватівка", "06:15", None, DAILY),
    ("Нова Водолага", "06:20", None, NO_SUNDAY),
    ("Безпалівка 1", "06:25", "08:30", DAILY),
    ("Утківка", "07:50", "08:50", NO_SUNDAY),
    ("Липкуватівка", None, "09:00", DAILY),
    ("Яковлівка", "07:40", "09:10", WORKDAYS),
    ("Рокитне", None, "09:50", NO_SUNDAY),
    ("Безпалівка 2", "08:15", "10:10", DAILY),
    ("Нова Водолага", None, "10:30", NO_SUNDAY),
    ("Соколово", "09:00", None, DAILY),
    ("Островерхівка", "09:30", "11:00", NO_SUNDAY),
    ("Утківка", "10:25", None, NO_SUNDAY),
    ("Соколово", None, "11:30", DAILY),
    ("Безпалівка 1", "10:25", "12:00", DAILY),
    ("Утківка", None, "12:30", NO_SUNDAY),
    ("Липкуватівка", "11:10", "13:00", NO_SUNDAY),
    ("Безпалівка 2", "11:35", "13:30", DAILY),
    ("Нова Водолага", "12:40", "14:30", NO_SUNDAY),
    ("Рокитне", "13:10", None, NO_SUNDAY),
    ("Рокитне", None, "15:15", NO_SUNDAY),
    ("Безпалівка 1", "13:55", "15:40", DAILY),
    ("Соколово", "14:10", "15:55", NO_SUNDAY),
    ("Островерхівка", "14:30", "16:05", DAILY),
    ("Безпалівка 2", "15:10", None, DAILY),
    ("Липкуватівка", None, "17:00", DAILY),
    ("Безпалівка 2", None, "17:30", DAILY),
    ("Нова Водолага", "16:00", "17:45", NO_SUNDAY),
    ("Утківка", "16:50", "18:00", NO_SUNDAY),
    ("Рокитне", "16:45", "18:10", NO_SUNDAY),
    # Рядок «Соколово, Островерхівка 17:00/17:30» — одна машина через обидва села.
    ("Соколово", "17:00", "19:10", NO_SUNDAY),
    ("Островерхівка", "17:30", "19:10", NO_SUNDAY),
]

# 1609 їде з самої Мерефи, тож у папірці початковий пункт у нього порожній.
TABLE_1609 = [
    ("06:10", "07:30", DAILY),
    ("07:30", None, NO_SUNDAY),
    ("08:20", "09:30", NO_SUNDAY),
    ("12:40", "14:00", DAILY),
    ("14:00", "15:00", DAILY),
    ("15:30", "16:40", DAILY),
    ("17:40", "18:40", DAILY),
    ("18:20", "19:30", NO_SUNDAY),
    ("19:10", "20:20", DAILY),
]

FILE_NAMES = {
    "Яковлівка": "yakovlivka-kharkiv",
    "Рокитне": "rokytne-kharkiv",
    "Утківка": "utkivka-kharkiv",
    "Соколово": "sokolovo-kharkiv",
    "Островерхівка": "ostroverkhivka-kharkiv",
    "Липкуватівка": "lypkuvativka-kharkiv",
    "Нова Водолага": "nova-vodolaha-kharkiv",
    "Безпалівка 1": "bezpalivka-1-kharkiv",
    "Безпалівка 2": "bezpalivka-2-kharkiv",
}

KHARKIV = "Харків (Холодна Гора)"


def empty_week():
    return {day: [] for day in DAILY}


def add(week, time, days):
    for day in days:
        week[day].append(time)


def sort_week(week):
    return {day: sorted(times) for day, times in week.items()}


def direction(label, stop, week):
    return {"label": label, "boardingStop": stop, "schedule": sort_week(week)}


def write(file_name, number, name, directions):
    path = ROUTES / f"{file_name}.json"
    payload = {"name": name, "directions": directions}
    if number:
        payload = {"number": number, **payload}
    text = json.dumps(payload, ensure_ascii=False, indent=2)
    # Списки часу тримаємо в один рядок, як у файлах, заповнених руками.
    text = re.sub(r"\[[\s\n]*((?:\"\d\d:\d\d\",?\s*)+)\]", lambda m: "[" + " ".join(m.group(1).split()) + "]", text)
    path.write_text(text + "\n", encoding="utf-8")
    return path


def main():
    outbound, inbound = {}, {}
    for route, from_origin, from_kharkiv, days in TABLE:
        outbound.setdefault(route, empty_week())
        inbound.setdefault(route, empty_week())
        if from_origin:
            add(outbound[route], from_origin, days)
        if from_kharkiv:
            add(inbound[route], from_kharkiv, days)

    for route in sorted(outbound):
        write(
            FILE_NAMES[route],
            None,
            f"{route} — Харків",
            [
                direction("На Харків", route, outbound[route]),
                direction("З Харкова", KHARKIV, inbound[route]),
            ],
        )

    out_1609, in_1609 = empty_week(), empty_week()
    for from_merefa, from_kharkiv, days in TABLE_1609:
        add(out_1609, from_merefa, days)
        if from_kharkiv:
            add(in_1609, from_kharkiv, days)

    write(
        "1609-merefa-kharkiv",
        "1609",
        "Мерефа — Харків",
        [
            direction("На Харків", "Мерефа (Селекційна)", out_1609),
            direction("З Харкова", KHARKIV, in_1609),
        ],
    )

    print(f"Записано {len(outbound) + 1} маршрутів")


if __name__ == "__main__":
    main()
