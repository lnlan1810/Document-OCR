package com.itis.ocrapp.domain.model

data class Passport(
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val dateOfIssue: String? = null,
    val dateOfExpiry: String? = null,
    val passportNumber: String? = null,
    val placeOfBirth: String? = null
)