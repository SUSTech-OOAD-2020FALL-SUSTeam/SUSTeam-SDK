package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonObjectOf

data class User(
    val username: String,
    val mail: String,
    val avatar: String?,
    val description: String?,
    val balance: Int
)

data class UserRole(
    val user: User,
    val roles: List<String>
)

fun User.toJson(): JsonObject = jsonObjectOf(
    "username" to username,
    "mail" to mail,
    "avatar" to (avatar ?: defaultAvatar()),
    "description" to description
)

fun JsonObject.toUser(): User = User(
    getString("username"),
    getString("mail"),
    getString("avatar"),
    getString("description"),
    getInteger("balance")
)

fun UserRole.toJson(): JsonObject = jsonObjectOf(
    "username" to user.username,
    "mail" to user.mail,
    "avatar" to (user.avatar ?: defaultAvatar()),
    "description" to user.description,
    "balance" to user.balance,
    "roles" to json { array(roles) }
)

fun defaultAvatar(): String = "/avatar/default.jpg"
