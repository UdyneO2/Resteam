package com.udyneos.resteam.domain.usecase

private val REPO = Regex("""[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}""")
private val BRANCH = Regex("""[A-Za-z0-9._/-]{1,100}""")
private val SCRIPT_FILE = Regex("""[A-Za-z0-9._-]{1,100}\.sh""")

/** "owner/repo". Divalidasi karena nilainya disisipkan ke URL. */
fun isValidRepo(value: String): Boolean = REPO.matches(value)

fun isValidBranch(value: String): Boolean =
    BRANCH.matches(value) && ".." !in value && !value.startsWith("/") && !value.endsWith("/")

fun isValidScriptFileName(value: String): Boolean = SCRIPT_FILE.matches(value) && ".." !in value
