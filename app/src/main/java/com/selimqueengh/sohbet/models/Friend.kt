package com.selimqueengh.sohbet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Friend(
    val name: String,
    val status: String,
    val avatar: String
) : Parcelable
