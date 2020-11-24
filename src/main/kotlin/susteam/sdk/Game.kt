package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import java.time.Instant

data class Game(
    val id: Int,
    val name: String,
    val price: Int,
    val publishDate: Instant,
    val author: String,
    val introduction: String?,
    val description: String?
)

fun Game.toJson(): JsonObject = jsonObjectOf(
    "id" to id,
    "name" to name,
    "price" to price,
    "publishDate" to publishDate,
    "author" to author,
    "introduction" to introduction,
    "description" to description
)

fun JsonObject.toGame(): Game = Game(
    getInteger("id"),
    getString("name"),
    getInteger("price"),
    getInstant("publishDate"),
    getString("author"),
    getString("introduction"),
    getString("description")
)
