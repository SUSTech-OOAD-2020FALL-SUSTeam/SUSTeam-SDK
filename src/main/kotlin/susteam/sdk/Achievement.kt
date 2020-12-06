package susteam.sdk

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

data class Achievement(
        val gameId: Int,
        val achievementId: Int,
        val achievementName: String,
        val description: String,
        val achieveCount: Int
)

fun Achievement.toJson(): JsonObject = jsonObjectOf(
        "gameId" to gameId,
        "achievementId" to achievementId,
        "achievementName" to achievementName,
        "description" to description,
        "achieveCount" to achieveCount
)


data class UserAchievementProcess(
        val username: String,
        val achievementId: Int,
        val rateOfProcess: Int,
        val finished: Boolean
)


fun UserAchievementProcess.toJson(): JsonObject = jsonObjectOf(
        "username" to username,
        "achievementId" to achievementId,
        "rateOfProcess" to rateOfProcess,
        "finished" to finished
)

fun JsonObject.toUserAchievementProcess(): UserAchievementProcess = UserAchievementProcess(
        getString("username"),
        getInteger("achievementId"),
        getInteger("rateOfProcess"),
        getBoolean("finished")
)
