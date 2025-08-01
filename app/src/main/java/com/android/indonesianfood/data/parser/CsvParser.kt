package com.android.indonesianfood.data.parser

import android.content.Context
import android.util.Log
import com.android.indonesianfood.data.model.FoodItem
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {
    fun parseFoodItems(context: Context, fileName: String): List<FoodItem> {
        val foodItems = mutableListOf<FoodItem>()
        try {
            val inputStream = context.assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = parseCsvLine(line!!)
                if (tokens.size >= 9) {
                    val id = tokens[0].trim()
                    val name = tokens[1].trim()
                    val category = tokens[2].trim()
                    val price = tokens[3].trim().toIntOrNull() ?: 0
                    val rating = tokens[4].trim().toDoubleOrNull() ?: 0.0
                    val origin = tokens[5].trim()
                    val imageUrl = tokens[6].trim()
                    val ingredients = tokens[7].split(";").map { it.trim() }.filter { it.isNotBlank() }
                    val instructions = tokens[8].split(";").map { it.trim() }.filter { it.isNotBlank() }

                    foodItems.add(
                        FoodItem(
                            id = id,
                            name = name,
                            category = category,
                            price = price,
                            rating = rating,
                            origin = origin,
                            imageUrl = imageUrl,
                            ingredients = ingredients,
                            instructions = instructions
                        )
                    )
                } else {
                    Log.w("CsvParser", "Skipping malformed row: $line (expected 9 columns, got ${tokens.size})")
                }
            }
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("CsvParser", "Error parsing CSV: ${e.message}")
        }
        return foodItems
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val char = line[i]
            when (char) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Double quote inside quoted field
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
