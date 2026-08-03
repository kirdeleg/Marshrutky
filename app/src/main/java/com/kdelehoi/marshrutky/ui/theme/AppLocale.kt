package com.kdelehoi.marshrutky.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.kdelehoi.marshrutky.domain.model.AppLanguage
import java.util.Locale

/**
 * Мова інтерфейсу поверх системної. Звичний спосіб — `AppCompatDelegate.setApplicationLocales` —
 * тут не підходить: він вимагає `AppCompatActivity` з темою AppCompat, а в нас чистий Compose із
 * власною темою. Тому підміняємо ресурси прямо в композиції: `stringResource` бере їх звідси, тож
 * мова перемикається миттєво, без перестворення активності й без мигання екрана.
 *
 * Системний вибір лишаємо як є — тоді Android сам вирішує за налаштуваннями телефону.
 */
@Composable
fun AppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localized = remember(language, context, configuration) {
        val tag = language.tag ?: return@remember context
        context.createConfigurationContext(
            Configuration(configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        )
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalResources provides localized.resources,
        LocalConfiguration provides localized.resources.configuration,
        content = content
    )
}
