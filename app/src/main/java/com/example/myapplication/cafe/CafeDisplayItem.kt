package com.example.myapplication.cafe

import androidx.annotation.DrawableRes

sealed class CafeDisplayItem

data class CafeCategory(
    val title: String
) : CafeDisplayItem()

data class CafeMenuItem(
    val name: String,
    @DrawableRes val imageResId: Int
) : CafeDisplayItem()