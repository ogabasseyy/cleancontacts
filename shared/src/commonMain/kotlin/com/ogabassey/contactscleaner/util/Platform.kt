package com.ogabassey.contactscleaner.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

expect fun getPlatformTimeMillis(): Long

@OptIn(ExperimentalUuidApi::class)
fun getPlatformUUID(): String = Uuid.random().toString()
