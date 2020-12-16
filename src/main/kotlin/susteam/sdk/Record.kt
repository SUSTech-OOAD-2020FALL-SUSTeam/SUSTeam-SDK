package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

data class Record(
        val recordId: Int,
        val gameId: Int,
        val username: String,
        val score: Int
)


fun Record.toJson(): JsonObject = jsonObjectOf(
        "recordId" to recordId,
        "gameId" to gameId,
        "username" to username,
        "score" to score,
)

fun JsonObject.toRecord(): Record = Record(
        getInteger("recordId"),
        getInteger("gameId"),
        getString("username"),
        getInteger("score")
)

