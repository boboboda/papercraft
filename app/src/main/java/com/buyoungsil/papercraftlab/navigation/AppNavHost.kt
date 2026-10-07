package com.buyoungsil.papercraftlab.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buyoungsil.papercraftlab.core.analytics.Analytics
import com.buyoungsil.papercraftlab.feature.gallery.GalleryScreen
import com.buyoungsil.papercraftlab.feature.menu.MenuScreen
import com.buyoungsil.papercraftlab.feature.play.PlayScreen

object Routes {
    const val MENU = "menu"
    const val GALLERY = "gallery"
    const val ARG_PICTURE_ID = "pictureId"
    const val PLAY = "play/{$ARG_PICTURE_ID}"

    fun play(pictureId: String) = "play/$pictureId"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // 화면 이동마다 screen_view ("play/{pictureId}" 는 "play" 로 묶는다)
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            Analytics.screen(entry.destination.route?.substringBefore('/') ?: "unknown")
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
        modifier = modifier
    ) {
        composable(Routes.MENU) {
            MenuScreen(
                onPlay = { id ->
                    navController.navigate(Routes.play(id)) { launchSingleTop = true }
                },
                onGallery = {
                    navController.navigate(Routes.GALLERY) { launchSingleTop = true }
                }
            )
        }

        composable(
            route = Routes.PLAY,
            arguments = listOf(navArgument(Routes.ARG_PICTURE_ID) { type = NavType.StringType })
        ) { entry ->
            val pictureId = entry.arguments?.getString(Routes.ARG_PICTURE_ID).orEmpty()
            PlayScreen(
                pictureId = pictureId,
                onBack = { navController.popBackStack() },
                onNext = { nextId ->                                            // ★
                    // 지금 플레이 화면을 빼고 다음 그림으로 → 뒤로 가면 바로 메뉴
                    navController.navigate(Routes.play(nextId)) {
                        popUpTo(Routes.PLAY) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(onBack = { navController.popBackStack() })
        }
    }
}