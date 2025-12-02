package com.example.agribotz.app.domain

import java.util.Date

data class MobileNumber(
    val country: String? = null,
    val number: String? = null
)

data class Mobile(
    val primary: MobileNumber,
    val isVerified: Boolean,
    val alternate: MobileNumber? = null
)

data class Email(
    val primary: String? = null,
    val isVerified: Boolean? = null,
    val alternate: String? = null
)

data class GPS(
    val lat: Float? = null,
    val long: Float? = null
)

data class Gadget(
    val id: String,
    val name: String,
    val gps: GPS? = null,
    val numberOfValves: Int?,
    val numberOfSensors: Int?,
    val variables: List<Variable>
)

data class Site(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val isActive: Boolean,
    val activatedAt: String? = null,
    val deactivatedAt: String? = null,
    val isTerminated: Boolean,
    val terminatedAt: String? = null,
    val numberOfGadgets: Int
)

data class LoginStatus(
    val failedTrials: Int,
    val nextTrial: String
)

data class ActiveStatus(
    val isSuspended: Boolean,
    val login: LoginStatus,
    val message: String? = null
)

data class User(
    val firstName: String,
    val lastName: String,
    val profilePhoto: String,
    val mobile: Mobile,
    val currency: String,
    val email: Email,
    val plan: String,
    val role: UserRole,
    val isActive: ActiveStatus,
    val createdAt: Date,
)

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(
        val devMessage: String,
        val userMessageKey: Int,
        val userMessageString: String? = null
    ) : ApiResult<Nothing>()
}

data class ErrorResponse(
    val status: String?,
    val error: String?
)

data class ScheduleValue(
    val frm: Long,
    val to: Long,
    val len: Int,
    val msk: Int
)

sealed class Variable {
    data class BooleanVar(val _id: String, val name: String, val label: String, val type: String, val value: Boolean, val updatedAt: String?, val category: String): Variable()
    data class IntegerVar(val _id: String, val name: String, val label: String, val type: String, val value: Int, val unit: String?, val updatedAt: String?, val category: String): Variable()
    data class FloatVar(val _id: String, val name: String, val label: String, val type: String, val value: Float, val unit: String?, val updatedAt: String?, val category: String): Variable()
    data class StringVar(val _id: String, val name: String, val label: String, val type: String, val value: String, val updatedAt: String?, val category: String): Variable()
    data class ScheduleVar(val _id: String, val name: String, val label: String, val type: String, val value: ScheduleValue, val updatedAt: String?, val category: String): Variable()
}

