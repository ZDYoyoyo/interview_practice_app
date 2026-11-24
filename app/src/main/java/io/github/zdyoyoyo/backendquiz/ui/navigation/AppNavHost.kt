package io.github.zdyoyoyo.backendquiz.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.zdyoyoyo.backendquiz.AppContainer
import io.github.zdyoyoyo.backendquiz.domain.quiz.QuizMode
import io.github.zdyoyoyo.backendquiz.ui.home.HomeScreen
import io.github.zdyoyoyo.backendquiz.ui.mistakes.MistakesScreen
import io.github.zdyoyoyo.backendquiz.ui.quiz.QuizScreen
import io.github.zdyoyoyo.backendquiz.ui.quiz.QuizSetupScreen
import io.github.zdyoyoyo.backendquiz.ui.settings.SettingsScreen
import io.github.zdyoyoyo.backendquiz.ui.stats.StatsScreen

/** 路由定義：畫面之間用「類似網址」的字串切換，參數放在 query string。 */
object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val MISTAKES = "mistakes"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val QUIZ = "quiz?mode={mode}&domains={domains}&count={count}"

    fun quiz(mode: QuizMode, domains: Set<String> = emptySet(), count: Int = 10): String =
        "quiz?mode=${mode.code}&domains=${Uri.encode(domains.joinToString(","))}&count=$count"
}

/** 一次複習最多幾題，避免積太多時一次做不完 */
private const val REVIEW_SESSION_MAX = 50

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onStartQuiz = { nav.navigate(Routes.SETUP) },
                onStartReview = { nav.navigate(Routes.quiz(QuizMode.REVIEW, count = REVIEW_SESSION_MAX)) },
                onOpenMistakes = { nav.navigate(Routes.MISTAKES) },
                onOpenStats = { nav.navigate(Routes.STATS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETUP) {
            QuizSetupScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onStart = { mode, domains, count ->
                    nav.navigate(Routes.quiz(mode, domains, count))
                },
            )
        }
        composable(Routes.MISTAKES) {
            MistakesScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onRedo = { nav.navigate(Routes.quiz(QuizMode.MISTAKES, count = 50)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onPracticeDomain = { domain ->
                    nav.navigate(Routes.quiz(QuizMode.WEAKNESS, setOf(domain.code), 10))
                },
            )
        }
        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = QuizMode.RANDOM.code },
                navArgument("domains") { type = NavType.StringType; defaultValue = "" },
                navArgument("count") { type = NavType.IntType; defaultValue = 10 },
            ),
        ) {
            QuizScreen(
                container = container,
                onExit = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
