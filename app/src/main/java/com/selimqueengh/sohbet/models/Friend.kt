package com.selimqueengh.sohbet.models

data class Friend(
    val name: String,
    val status: String,
    val avatar: String,
    val userId: String = ""
)
