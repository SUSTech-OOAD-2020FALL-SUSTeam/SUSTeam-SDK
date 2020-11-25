package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import java.time.Instant

data class GameSave (
    val username: String,
    val gameId: Int,
    val saveName: String,
    val savedTime: Instant
)

fun GameSave.toJson(): JsonObject = jsonObjectOf(
    "username" to username,
    "gameId" to gameId,
    "saveName" to saveName,
    "savedTime" to savedTime
)

fun JsonObject.toGameSave(): GameSave = GameSave(
    getString("username"),
    getInteger("gameId"),
    getString("saveName"),
    getInstant("savedTime")
)
