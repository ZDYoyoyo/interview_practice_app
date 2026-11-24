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

/** 路由定義：畫面之間用「類似網址」的字串切換，參數放在 query string。 */
object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val MISTAKES = "mistakes"
    const val QUIZ = "quiz?mode={mode}&domains={domains}&count={count}"

    fun quiz(mode: QuizMode, domains: Set<String> = emptySet(), count: Int = 10): String =
        "quiz?mode=${mode.code}&domains=${Uri.encode(domains.joinToString(","))}&count=$count"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onStartQuiz = { nav.navigate(Routes.SETUP) },
                onOpenMistakes = { nav.navigate(Routes.MISTAKES) },
            )
        }
        composable(Routes.SETUP) {
            QuizSetupScreen(
                container = container,
                onBack = { nav.popBackStack() },
                onStart = { domains, count ->
                    nav.navigate(Routes.quiz(QuizMode.RANDOM, domains, count))
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
