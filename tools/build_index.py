#!/usr/bin/env python3
"""Перебудовує routes/index.json — перелік файлів розкладів із хешами вмісту.

Застосунок читає цей файл замість Contents API GitHub: у того ліміт 60 запитів на годину на
IP без токена, і за спільним NAT оператора його вичерпує кілька людей. Роздача через
raw.githubusercontent ліміту не має.

Хеш — той самий SHA-1 блоба, що його рахує git (і що його колись віддавав Contents API),
тому вміст індексу можна перевірити командою `git hash-object routes/файл.json`.

Запускати після кожної зміни в routes/, інакше застосунок не побачить нового маршруту або
піде по видалений файл. Те саме перевіряє RoutesIndexTest, тож несвіжий індекс валить тести.
"""

import hashlib
import json
import pathlib

ROUTES = pathlib.Path(__file__).resolve().parent.parent / "routes"
INDEX = ROUTES / "index.json"


def blob_sha(path: pathlib.Path) -> str:
    content = path.read_bytes()
    return hashlib.sha1(b"blob %d\0" % len(content) + content).hexdigest()


def main() -> None:
    files = sorted(p for p in ROUTES.glob("*.json") if p != INDEX)
    index = {"routes": [{"file": p.name, "sha": blob_sha(p)} for p in files]}

    INDEX.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{INDEX.relative_to(ROUTES.parent)}: {len(files)} маршрутів")


if __name__ == "__main__":
    main()
