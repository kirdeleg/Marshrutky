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
     * Перелік файлів беремо з індексу в самому репозиторії, а не з Contents API: у того ліміт 60
     * запитів на годину на IP без токена, а за спільним NAT оператора одну адресу ділять сотні
     * людей. Вичерпаний ліміт — це не «оновимося пізніше», а порожній застосунок у того, хто
     * запустив його вперше й кешу ще не має. Роздача файлів через raw.githubusercontent ліміту не
     * має взагалі.
     */
    val indexUrl: String
        get() = fileUrl(INDEX_FILE_NAME)

    fun fileUrl(fileName: String): String =
        "https://raw.githubusercontent.com/$owner/$repository/$branch/$directory/$fileName"

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

/** Перелік файлів із хешами вмісту. Лежить поруч із розкладами й оновлюється разом із ними. */
const val INDEX_FILE_NAME = "index.json"
