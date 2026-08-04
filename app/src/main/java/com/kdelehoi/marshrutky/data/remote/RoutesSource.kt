package com.kdelehoi.marshrutky.data.remote

/**
 * Звідки беремо розклади. Окремим типом, а не літералом усередині джерела даних: наступний крок —
 * кілька репозиторіїв на різні напрямки, і тоді джерело обиратиме користувач у параметрах.
 */
data class RoutesSource(
    val owner: String,
    val repository: String,
    val branch: String,
    val directory: String
) {
    /**
     * Список файлів через Contents API: новий маршрут з'являється в застосунку одразу після пуша, без
     * жодних індексів.
     */
    val contentsUrl: String
        get() = "https://api.github.com/repos/$owner/$repository/contents/$directory?ref=$branch"

    companion object {
        val Default = RoutesSource(
            owner = "kirdeleg",
            repository = "Marshrutky",
            branch = "main",
            directory = "routes"
        )
    }
}

/** Розклади лежать у JSON, і саме за цим суфіксом ми відрізняємо їх від решти файлів у теці. */
const val ROUTE_FILE_SUFFIX = ".json"
