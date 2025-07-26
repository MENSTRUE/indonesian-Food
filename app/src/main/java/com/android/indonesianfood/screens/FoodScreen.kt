package com.android.indonesianfood.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.indonesianfood.components.FoodItemCard
import com.android.indonesianfood.data.viewmodel.HomeViewModel
import com.android.indonesianfood.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    onFoodItemClick: (String) -> Unit,
    onBackPressExit: () -> Unit, // Tambahkan callback exit
    homeViewModel: HomeViewModel = viewModel()
) {
    BackHandler(enabled = true) {
        // Jika di FoodScreen dan tidak ada lagi di back stack utama, keluar aplikasi
        if (navController.currentBackStackEntry?.destination?.route == Routes.FOOD_LIST && navController.previousBackStackEntry == null) {
            onBackPressExit()
        } else {
            navController.popBackStack()
        }
    }

    val foodItems by homeViewModel.filteredFoodItems.observeAsState(emptyList())
    val isLoading by homeViewModel.isLoading.observeAsState(true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Makanan", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (foodItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada resep ditemukan.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(paddingValues)
            ) {
                items(foodItems, key = { it.id }) { foodItem ->
                    FoodItemCard(foodItem = foodItem, onClick = onFoodItemClick)
                }
            }
        }
    }
}