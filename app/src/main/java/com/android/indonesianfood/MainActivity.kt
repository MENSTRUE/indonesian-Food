package com.android.indonesianfood

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.indonesianfood.navigation.Routes
import com.android.indonesianfood.screens.DetailScreen
import com.android.indonesianfood.screens.EditProfileScreen
import com.android.indonesianfood.screens.FoodScreen
import com.android.indonesianfood.screens.HomeScreen
import com.android.indonesianfood.screens.IngredientTrackingScreen
import com.android.indonesianfood.screens.LoginScreen
import com.android.indonesianfood.screens.ProfileScreen
import com.android.indonesianfood.screens.SearchScreen
import com.android.indonesianfood.ui.theme.IndonesianFoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IndonesianFoodTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodAppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current // Mendapatkan context

    val bottomNavItems = listOf(
        BottomNavItem("Beranda", Icons.Default.Home, Routes.HOME),
        BottomNavItem("Makanan", Icons.Default.Restaurant, Routes.FOOD_LIST),
        BottomNavItem("Tracking", Icons.Default.CameraAlt, Routes.TRACKING),
        BottomNavItem("Cari", Icons.Default.Search, Routes.SEARCH),
        BottomNavItem("Profil", Icons.Default.Person, Routes.PROFILE)
    )

    val topLevelRoutes = setOf(Routes.HOME, Routes.FOOD_LIST, Routes.TRACKING, Routes.SEARCH, Routes.PROFILE)
    val shouldShowBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRegisterClick = { }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    paddingValues = PaddingValues(0.dp),
                    onBackPressExit = { (context as? Activity)?.finish() }, // Meneruskan callback exit
                    navController = navController // Meneruskan navController
                )
            }
            composable(Routes.FOOD_LIST) {
                FoodScreen(
                    paddingValues = PaddingValues(0.dp),
                    navController = navController,
                    onFoodItemClick = { foodItemId ->
                        navController.navigate(Routes.DETAIL.replace("{foodItemId}", foodItemId))
                    },
                    onBackPressExit = { (context as? Activity)?.finish() } // Meneruskan callback exit
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(androidx.navigation.navArgument("foodItemId") {
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val foodItemId = backStackEntry.arguments?.getString("foodItemId")
                DetailScreen(foodItemId = foodItemId, onBackClick = { navController.popBackStack() })
            }
            composable(Routes.TRACKING) {
                IngredientTrackingScreen(
                    onBackClick = { navController.popBackStack() },
                    paddingValues = PaddingValues(0.dp),
                    onBackPressExit = { (context as? Activity)?.finish() } // Meneruskan callback exit
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    navController = navController,
                    paddingValues = PaddingValues(0.dp),
                    onBackPressExit = { (context as? Activity)?.finish() } // Meneruskan callback exit
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                    paddingValues = PaddingValues(0.dp)
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    IndonesianFoodTheme {
        FoodAppNavigation()
    }
}