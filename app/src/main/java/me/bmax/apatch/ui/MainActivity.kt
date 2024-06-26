package me.bmax.apatch.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.NestedNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.isRouteOnBackStack
import me.bmax.apatch.APApplication
import me.bmax.apatch.ui.component.rememberDialogHostState
import me.bmax.apatch.ui.screen.BottomBarDestination
import me.bmax.apatch.ui.screen.NavGraphs
import me.bmax.apatch.ui.theme.APatchTheme
import me.bmax.apatch.util.LocalDialogHost
import me.bmax.apatch.util.LocalSnackbarHost

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            APatchTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val route = navBackStackEntry?.destination?.route
                val showBottomBar = route == null || !route.startsWith("web_screen")

                val navHostEngine = rememberNavHostEngine(
                    navHostContentAlignment = Alignment.TopCenter,
                    rootDefaultAnimations = RootNavGraphDefaultAnimations(
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ),
                    defaultAnimationsForNestedNavGraph = mapOf(
                        NavGraphs.root to NestedNavGraphDefaultAnimations(
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) }
                        ),
                    )
                )
                Scaffold(
                    bottomBar = { if (showBottomBar) BottomBar(navController) },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalSnackbarHost provides snackbarHostState,
                        LocalDialogHost provides rememberDialogHostState(),
                    ) {
                        DestinationsNavHost(
                            modifier = Modifier.padding(innerPadding),
                            navGraph = NavGraphs.root,
                            navController = navController,
                            engine = navHostEngine
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    NavigationBar(tonalElevation = 8.dp) {
        BottomBarDestination.entries.forEach { destination ->
            val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
            val kPatchReady = state != APApplication.State.UNKNOWN_STATE
            val aPatchReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING ||
                    state == APApplication.State.ANDROIDPATCH_INSTALLED ||
                    state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)
            val hideDestination = (destination.kPatchRequired && !kPatchReady) ||
                                  (destination.aPatchRequired && !aPatchReady)
            if (hideDestination) return@forEach
            val isCurrentDestOnBackStack = navController.isRouteOnBackStack(destination.direction)
            NavigationBarItem(
                selected = isCurrentDestOnBackStack,
                onClick = {
                    if (isCurrentDestOnBackStack) {
                        navController.popBackStack(destination.direction, false)
                    }

                    navController.navigate(destination.direction.route) {
                        popUpTo(NavGraphs.root.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (isCurrentDestOnBackStack) {
                        Icon(destination.iconSelected, stringResource(destination.label))
                    } else {
                        Icon(destination.iconNotSelected, stringResource(destination.label))
                    }
                },
                label = { Text(stringResource(destination.label)) },
                alwaysShowLabel = false
            )
        }
    }
}
