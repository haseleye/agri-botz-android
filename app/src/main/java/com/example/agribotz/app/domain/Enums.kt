package com.example.agribotz.app.domain

import com.google.gson.annotations.SerializedName

enum class UserRole() {
    @SerializedName("ADMIN") ADMIN,
    @SerializedName("USER") USER,
    @SerializedName("AGENT") AGENT
}

enum class VarCategory() {
    @SerializedName("SENSOR") SENSOR,
    @SerializedName("IRRIGATION") IRRIGATION,
    @SerializedName("INDICATOR") INDICATOR,
    @SerializedName("COMMAND") COMMAND,
    @SerializedName("SYSTEM") SYSTEM,
    @SerializedName("SETTING") SETTING
}

enum class ApiStatus {
    LOADING,
    ERROR,
    DONE
}

