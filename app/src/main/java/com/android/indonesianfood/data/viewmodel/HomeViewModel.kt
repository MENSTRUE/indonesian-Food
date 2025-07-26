package com.android.indonesianfood.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.indonesianfood.data.model.FoodItem
import com.android.indonesianfood.data.parser.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _foodItems = MutableLiveData<List<FoodItem>>()
    val foodItems: LiveData<List<FoodItem>> get() = _foodItems

    private val _filteredFoodItems = MutableLiveData<List<FoodItem>>()
    val filteredFoodItems: LiveData<List<FoodItem>> get() = _filteredFoodItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _userName = MutableLiveData("FoodLover123")
    val userName: LiveData<String> get() = _userName

    private val _userEmail = MutableLiveData("user@example.com")
    val userEmail: LiveData<String> get() = _userEmail

    init {
        loadFoodItems()
    }

    private fun loadFoodItems() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val loadedItems = CsvParser.parseFoodItems(getApplication(), "Indonesian_Food_Dataset.csv")
            withContext(Dispatchers.Main) {
                _foodItems.value = loadedItems
                _filteredFoodItems.value = loadedItems // Awalnya tampilkan semua
                _isLoading.value = false
            }
        }
    }

    fun getRecommendedFoodItems(count: Int = 5): List<FoodItem> {
        return _foodItems.value
            ?.sortedByDescending { it.rating }
            ?.take(count)
            ?: emptyList()
    }

    fun getFoodItemById(id: String): FoodItem? {
        return _foodItems.value?.find { it.id == id }
    }

    fun performSearch(query: String) {
        val currentList = _foodItems.value ?: return
        if (query.isBlank()) {
            _filteredFoodItems.value = currentList
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            _filteredFoodItems.value = currentList.filter {
                it.name.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        it.category.lowercase(Locale.getDefault()).contains(lowerCaseQuery)
            }
        }
    }

    fun updateUserData(newName: String, newEmail: String) {
        _userName.value = newName
        _userEmail.value = newEmail
    }
}