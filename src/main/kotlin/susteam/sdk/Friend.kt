package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import java.time.Instant

data class Friend(
    val username: String,
    val online: Boolean,
    val lastSeen: Instant?
)

fun Friend.toJson(): JsonObject = jsonObjectOf(
    "username" to username,
    "online" to online
).also {
    if (lastSeen != null) it.put("lastSeen", lastSeen)
}

data class FriendApplication(
    val to: String,
    val status: String
)

fun FriendApplication.toJson(): JsonObject = jsonObjectOf(
    "to" to to,
    "status" to status
)

fun JsonObject.toFriendApplication(): FriendApplication = FriendApplication(
    getString("to"),
    getString("status")
)

data class FriendReply(
    val from: String,
    val status: String
)

fun FriendReply.toJson(): JsonObject = jsonObjectOf(
    "from" to from,
    "status" to status
)

fun JsonObject.toFriendReply(): FriendReply = FriendReply(
    getString("from"),
    getString("status")
)
