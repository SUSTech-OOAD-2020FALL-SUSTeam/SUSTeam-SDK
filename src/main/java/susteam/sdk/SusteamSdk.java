package susteam.sdk;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public class SusteamSdk {

    private static final String SERVER_HOST = "susteam.gogo.moe";

    private static Vertx vertx;
    private static WebClient client;
    private static String token;
    private static String gameKey;

    /**
     * Init sdk
     *
     * @param token User token
     *
     * @param gameKey Key of the game, provided when creating a game
     */
    public static void init(String token, String gameKey) {
        vertx = Vertx.vertx();
        client = WebClient.create(
                vertx,
                new WebClientOptions().setDefaultHost(SERVER_HOST)
        );
        SusteamSdk.token = token;
        SusteamSdk.gameKey = gameKey;
    }

    /**
     * Get Game Future (including fields: gameId, name, author,
     * publishTime, introduction and description) according to the game key
     *
     * @return the future
     */
    public static Future<Game> getGame() {
        Promise<Game> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/gameKey/" + SusteamSdk.gameKey);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            if (result.result().bodyAsJsonObject().getBoolean("success")) {
                Game game = GameKt.toGame(result.result().bodyAsJsonObject().getJsonObject("game"));
                promise.complete(game);
            } else {
                promise.fail(result.result().bodyAsJsonObject().getString("error"));
            }
        });
        return promise.future();
    }

    /**
     * Create a void future whether server is online
     *
     * Server is online if the future is succeeded.
     *
     * @return the future
     */
    public static Future<Void> isServerOnline() {
        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (token != null) {
                promise.complete();
            } else {
                promise.fail("json format invalid");
            }
        });
        return promise.future();
    }

    /**
     * Get User Future (User, contains username, mail,
     * avatar, description) according to current user
     *
     * @return the future
     */
    public static Future<User> user() {
        Promise<User> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            promise.complete(user);
        });
        return promise.future();
    }

    /**
     * Get GameSave Array Future including all game saves of current user in game
     *
     * @return the future
     */
    public static Future<GameSave[]> getAllGameSaveName() {
        Promise<GameSave[]> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }
            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();

            client.get("/api/save/" + username + "/" + SusteamSdk.gameKey)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonArray saves = res.result().bodyAsJsonObject().getJsonArray("gameSaves");
                                GameSave[] gameSaves = new GameSave[saves.size()];
                                for (int i = 0; i < saves.size(); i++) {
                                    gameSaves[i] = new GameSave(
                                            saves.getJsonObject(i).getString("username"),
                                            saves.getJsonObject(i).getInteger("gameId"),
                                            saves.getJsonObject(i).getString("saveName"),
                                            saves.getJsonObject(i).getInstant("savedTime")
                                    );
                                }
                                promise.complete(gameSaves);
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }

    /**
     * Delete a game save and return a void future.
     *
     * Delete successfully if the future is succeeded.
     *
     * @param fileName The file name of a game save
     *
     * @return A void future
     */
    public static Future<Void> deleteSave(String fileName) {
        Promise<Void> promise = Promise.promise();
            HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
            request.send(result -> {
                if (result.failed()) {
                    promise.fail(result.cause());
                    return;
                }
                Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
                if (!token) {
                    promise.fail("token invalid");
                    return;
                }
                User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
                String username = user.getUsername();

                client.get("/api/save/" + username + "/" + SusteamSdk.gameKey + "/" + fileName + "/delete")
                        .bearerTokenAuthentication(SusteamSdk.token)
                        .send(res -> {
                            if (res.succeeded()) {
                                if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                    promise.complete();
                                } else {
                                    promise.fail(res.result().bodyAsJsonObject().getString("error"));
                                }
                            } else {
                                promise.fail(res.cause());
                            }

                        });
            });
        return promise.future();
    }

    /**
     * Save a file and return a void future. The file should first be saved in local dir, then pass the file to server.
     *
     * Save successfully if the future is succeeded.
     *
     * @param file File need to be saved
     *
     * @return A void future
     */
    public static Future<Void> save(File file) {

        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }
            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();
            final MultipartForm form = MultipartForm.create();
            try {
                form.textFileUpload("txt-file", file.getName(), file.getAbsolutePath(), "text/plain");
            } catch (Exception e) {
                promise.fail("file open fail");
                return;
            }
            client
                    .post("/api/save/" + username + "/" + SusteamSdk.gameKey + "/" + file.getName())
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .sendMultipartForm(form, res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                promise.complete();
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }
                    });
        });
        return promise.future();
    }

    /**
     * Load a file and return a void future
     *
     * Load successfully if the future is succeeded.
     *
     * @param fileName The file name need to load from server
     *
     * @return the future
     */
    public static Future<File> load(String fileName) {

        Promise<File> promise = Promise.promise();

        SusteamSdk.getGame().onComplete(it -> {
            if (it.failed()) {
                promise.fail(it.cause());
                return;
            }
            Game game = it.result();
            HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
            request.send(result -> {
                if (result.failed()) {
                    promise.fail(result.cause());
                    return;
                }
                Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
                if (!token) {
                    promise.fail("token invalid");
                    return;
                }

                User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
                String username = user.getUsername();
                HttpRequest<Buffer> loadRequest =
                        client.get("/api/save/" + username + "/" + SusteamSdk.gameKey + "/" + fileName)
                                .bearerTokenAuthentication(SusteamSdk.token);

                loadRequest.send(res -> {
                    if (res.failed()) {
                        promise.fail(res.cause());
                        return;
                    }
                    File dir = new File(System.getProperty("java.io.tmpdir") + "susteam/sdk/" + game.getId());
                    dir.mkdirs();
                    File file = new File(dir + "/" + fileName);

                    try {
                        file.createNewFile();
                        Files.write(file.toPath(), res.result().bodyAsBuffer().getBytes(), StandardOpenOption.WRITE);
                        promise.complete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.fail("file error");
                    }
                });
            });
        });
        return promise.future();
    }

    /**
     * Update an achievement in the game and return a void future
     *
     * Update successfully if the future is succeeded.
     *
     * @param achievementName The achievement name
     *
     * @param rateOfProcess User current process
     *
     * @return the future
     *
     */
    public static Future<Void> updateUserAchievementProcess(String achievementName, int rateOfProcess) {

        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("username", username);
            jsonObject.put("achievementName", achievementName);
            jsonObject.put("rateOfProcess", rateOfProcess);

            client
                    .post("/api/achieveProcess/" + gameKey)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .sendJson(jsonObject, res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                promise.complete();
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }
                    });
        });
        return promise.future();
    }

    /**
     * Add an achievement in the game and return a void future
     *
     * Add successfully if the future is succeeded.
     *
     * @param achievementName The name of achievement
     *
     * @param description Description of achievement
     *
     * @param achievementCount Total count need to finished
     *
     * @return Void
     */
    public static Future<Void> addAchievement(String achievementName, String description, int achievementCount) {

        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.put("achievementName", achievementName);
            jsonObject.put("description", description);
            jsonObject.put("achievementCount", achievementCount);

            client
                    .post("/api/achievement/" + SusteamSdk.gameKey)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .sendJson(jsonObject, res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                promise.complete();
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }
                    });
        });
        return promise.future();
    }

    /**
     * Get Achievement Array Future including all achievements of current user in game
     *
     * @return the future
     */
    public static Future<Achievement[]> getAllAchievement() {
        Promise<Achievement[]> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            client.get("/api/achievement/" + SusteamSdk.gameKey)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonArray achievements = res.result().bodyAsJsonObject().getJsonArray("achievements");
                                Achievement[] gameAchievements = new Achievement[achievements.size()];
                                for (int i = 0; i < achievements.size(); i++) {
                                    gameAchievements[i] = new Achievement(
                                            achievements.getJsonObject(i).getInteger("gameId"),
                                            achievements.getJsonObject(i).getInteger("achievementId"),
                                            achievements.getJsonObject(i).getString("achievementName"),
                                            achievements.getJsonObject(i).getString("description"),
                                            achievements.getJsonObject(i).getInteger("achieveCount")
                                    );
                                }
                                promise.complete(gameAchievements);
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }

    /**
     * Get Achievement Future according to achievementName of current user
     *
     * @param achievementName Name of achievement
     *
     * @return the future
     */
    public static Future<Achievement> achievement(String achievementName) {
        Promise<Achievement> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            client.get("/api/achievement/" + SusteamSdk.gameKey + "/" + URLEncoder.encode(achievementName, StandardCharsets.UTF_8))
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonObject achievements = res.result().bodyAsJsonObject().getJsonObject("achievement");
                                Achievement gameAchievement = new Achievement(
                                        achievements.getInteger("gameId"),
                                        achievements.getInteger("achievementId"),
                                        achievements.getString("achievementName"),
                                        achievements.getString("description"),
                                        achievements.getInteger("achieveCount")
                                );
                                promise.complete(gameAchievement);
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }

    /**
     * Get an Integer future representing current process of this achievement in the game
     *
     * @param achievementName Name of achievement
     *
     * @return the future
     */
    public static Future<Integer> getUserAchievementProcess(String achievementName) {
        Promise<Integer> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();

            client.get("/api/achieveProcess/" + username + "/" + SusteamSdk.gameKey + "/" + URLEncoder.encode(achievementName, StandardCharsets.UTF_8))
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                promise.complete(res.result().bodyAsJsonObject().getJsonObject("userAchievementProcess").getInteger("rateOfProcess"));
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }

    /**
     * Get Record Array Future representing the records whose score are
     * in rank of the game
     *
     * @param rankNum Number of records
     *
     * @return the future
     */
    public static Future<Record[]> getRank(int rankNum) {
        Promise<Record[]> promise = Promise.promise();

        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();

            client.get("/api/record/" + SusteamSdk.gameKey + "/" + rankNum)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonArray records = res.result().bodyAsJsonObject().getJsonArray("records");
                                Record[] gameRecords = new Record[records.size()];
                                for (int i = 0; i < records.size(); i++) {
                                    gameRecords[i] = new Record(
                                            records.getJsonObject(i).getInteger("recordId"),
                                            records.getJsonObject(i).getInteger("gameId"),
                                            records.getJsonObject(i).getString("username"),
                                            records.getJsonObject(i).getInteger("score")
                                    );
                                }
                                promise.complete(gameRecords);
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }


    /**
     * Get Integer Future representing the max score of current user in the game
     *
     * @return the future
     */
    public static Future<Integer> getUserMaxScore() {
        Promise<Integer> promise = Promise.promise();

        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();

            client.get("/api/record/max/" + SusteamSdk.gameKey + "/" + username)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send(res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonObject jsonObject = res.result().bodyAsJsonObject().getJsonObject("record");
                                Record record = new Record(
                                            jsonObject.getInteger("recordId"),
                                            jsonObject.getInteger("gameId"),
                                            jsonObject.getString("username"),
                                            jsonObject.getInteger("score")
                                    );
                                promise.complete(record.getScore());
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                       promise.fail(res.cause());
                        }

                    });
        });
        return promise.future();
    }

    /**
     * Upload a score record and return a void future.
     *
     * Upload successfully if the future is succeeded.
     *
     * @param score The record need to upload
     *
     * @return the future
     */
    public static Future<Void> addRecord(int score) {

        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/token").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            Boolean token = result.result().bodyAsJsonObject().getBoolean("token");
            if (!token) {
                promise.fail("token invalid");
                return;
            }

            User user = UserKt.toUser(result.result().bodyAsJsonObject().getJsonObject("userRole"));
            String username = user.getUsername();

            JsonObject jsonObject = new JsonObject();
            jsonObject.put("username", username);
            jsonObject.put("score", score);

            client
                    .post("/api/record/" + gameKey)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .sendJson(jsonObject, res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                promise.complete();
                            } else {
                                promise.fail(res.result().bodyAsJsonObject().getString("error"));
                            }
                        } else {
                            promise.fail(res.cause());
                        }
                    });
        });
        return promise.future();
    }


    /**
     * Get Friend Array Future representing all friends of current user
     *
     * @return the future
     */
    public static Future<Friend[]> friends() {
        Promise<Friend[]> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/friend").bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }

            if (result.result().bodyAsJsonObject().getBoolean("success")) {
                JsonArray jsonObjectFriends = result.result().bodyAsJsonObject().getJsonArray("friends");
                Friend[] friends = new Friend[jsonObjectFriends.size()];
                for (int i = 0; i < jsonObjectFriends.size(); i++) {
                    friends[i] = new Friend(
                            jsonObjectFriends.getJsonObject(i).getString("username"),
                            jsonObjectFriends.getJsonObject(i).getBoolean("online"),
                            jsonObjectFriends.getJsonObject(i).getInstant("lastSeen")
                    );
                }
                promise.complete(friends);
            } else {
                promise.fail(result.result().bodyAsJsonObject().getString("error"));
            }
        });
        return promise.future();
    }

    /**
     * Get Friend Array Future representing all friends of current user
     * who who have already bought this game
     *
     * @return the future
     */
    public static Future<Friend[]> gameFriends() {
        Promise<Friend[]> promise = Promise.promise();
        SusteamSdk.getGame().onComplete(it -> {
            if (it.failed()) {
                promise.fail(it.cause());
                return;
            }
            Game game = it.result();
            HttpRequest<Buffer> request = client.get("/api/friend/" + game.getId()).bearerTokenAuthentication(token);
            request.send(result -> {
                if (result.failed()) {
                    promise.fail(result.cause());
                    return;
                }

                if (result.result().bodyAsJsonObject().getBoolean("success")) {
                    JsonArray jsonObjectFriends = result.result().bodyAsJsonObject().getJsonArray("friends");
                    Friend[] friends = new Friend[jsonObjectFriends.size()];
                    for (int i = 0; i < jsonObjectFriends.size(); i++) {
                        friends[i] = new Friend(
                                jsonObjectFriends.getJsonObject(i).getString("username"),
                                jsonObjectFriends.getJsonObject(i).getBoolean("online"),
                                jsonObjectFriends.getJsonObject(i).getInstant("lastSeen")
                        );
                    }
                    promise.complete(friends);
                } else {
                    promise.fail(result.result().bodyAsJsonObject().getString("error"));
                }
            });
        });
        return promise.future();
    }

    /**
     * Send an invite message to friend and return a void future.
     *
     * Send successfully if the future is succeeded.
     *
     * @param friendName Name of the friend, who must online
     *
     * @return the future
     */
    public static Future<Void> invite(String friendName) {
        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/friend/invite/" + friendName + "/" + SusteamSdk.gameKey).bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            if (result.result().bodyAsJsonObject().getBoolean("success")) {
                promise.complete();
            } else {
                promise.fail(result.result().bodyAsJsonObject().getString("error"));
            }
        });
        return promise.future();
    }

    /**
     * Check whether user have bought the game
     *
     * @param gameKey Game key of the game
     * @return Whether user have bought it
     */
    public static Future<Boolean> checkBought(String gameKey) {
        Promise<Boolean> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/check/key/" + gameKey).bearerTokenAuthentication(token);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            if (result.result().bodyAsJsonObject().getBoolean("success")) {
                promise.complete(result.result().bodyAsJsonObject().getBoolean("bought"));
            } else {
                promise.fail(result.result().bodyAsJsonObject().getString("error"));
            }
        });
        return promise.future();
    }
//    public static void main(String[] args) {
//        SusteamSdk.init(
//                "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3QwMDEiLCJwZXJtaXNzaW9ucyI6W10sImlhdCI6MTYwNTcwNzIxNn0.jo9VGmhssPLcKBvU2RfQOGTIsPnd1g-t5LD2ZI-ftqmEBJY06I0a5_kXN1Qc31AoUSwDNEp3JLY0Xku0-faw1DQGOSUUJLKf2wnvzY-36ZoGgVDgZEVgwfKuTyGL-uLuJevV3o4CBpcWx4XdJ0sbogx2oAszV1MR6n7bvSyIjPu368-cdRK4qZ_5Yrk9vfb88D8bH8SGR7AC7JINZam7YnFenk-0DDRDztYaQCgQn356Fz29Lzke3DOXw7gSQm1KPP2MQVJrCkUuZdPckl9PCCN7lj8xm8RM0C0H8B7ozp22qHhztqbcBRW0hXtycSlQ3k-QjdTv5P31_pZGwF7TxQ",
//                "o6cf3Wd9OXOvzq9pRdBB4EeYBpimP0X1WwFBSOgLpajJ3MutNmsVWjDWjX5Vz8bVavbix4Ya2gyDVLHNgjIX3toZKOkuVkAM8sMMD");
//
//        SusteamSdk.isServerOnline().onComplete(it -> {
//            if (it.succeeded()) {
//                System.out.println("server is online");
//            } else {
//                System.out.println("server is not online");
//            }
//        });
//
//        SusteamSdk.gameFriends();
//        SusteamSdk.invite("yinpeiqi");
//        SusteamSdk.addAchievement("test","test",1);
//        SusteamSdk.user().onSuccess(it -> {
//            System.out.println(it.getUsername());
//        });
//        SusteamSdk.getGame(10);
//        SusteamSdk.save(new File("C:\\Users\\yinpe\\IdeaProjects\\SUSTeam-SDK\\testfile.txt"))
//                .onComplete(it -> {
//                    System.out.println("success");
//                });
//
//
//        SusteamSdk.addRecord(1055);
//        SusteamSdk.getUserMaxScore();
//        SusteamSdk.getRank(10);
//
//    }

}
