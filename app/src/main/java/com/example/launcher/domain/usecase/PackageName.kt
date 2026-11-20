package com.example.launcher.domain.usecase

private val PACKAGE_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

/** Validasi nama package sebelum dipakai sebagai argumen perintah shell. */
internal fun isValidPackageName(name: String): Boolean = PACKAGE_REGEX.matches(name)
