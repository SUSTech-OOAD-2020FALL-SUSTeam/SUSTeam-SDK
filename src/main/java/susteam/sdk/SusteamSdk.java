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

    public static final String SERVER_HOST = "susteam.gogo.moe";

    public static Vertx vertx;
    public static WebClient client;
    public static String token;
    public static int gameId;

    public static void init(String token, String gameKey) {
        vertx = Vertx.vertx();
        client = WebClient.create(
                vertx,
                new WebClientOptions().setDefaultHost(SERVER_HOST)
        );
        SusteamSdk.token = token;
        SusteamSdk.getGameId(gameKey).onComplete(it -> {
            if ( it.succeeded() ) {
                SusteamSdk.gameId = it.result();
            }
        });
    }

    public static Future<Integer> getGameId(String gameKey) {
        Promise<Integer> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/gameKey/" + gameId);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            if (result.result().bodyAsJsonObject().getBoolean("success")) {
                int gameId = result.result().bodyAsJsonObject().getInteger("gameId");
                promise.complete(gameId);
            } else {
                promise.fail(result.result().bodyAsJsonObject().getString("error"));
            }
        });
        return promise.future();
    }

    public static Future<Game> getGame(int gameId) {
        Promise<Game> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/game/" + gameId);
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

            client.get("/api/save/" + username + "/" + gameId)
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

            client.get("/api/save/" + username + "/" + gameId + "/" + fileName + "/delete")
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
                    .post("/api/save/" + username + "/" + gameId + "/" + file.getName())
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


    public static Future<File> load(String fileName) {

        Promise<File> promise = Promise.promise();
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
                    client.get("/api/save/" + username + "/" + gameId + "/" + fileName)
                            .bearerTokenAuthentication(SusteamSdk.token);

            loadRequest.send(res -> {
                if (res.failed()) {
                    promise.fail(res.cause());
                    return;
                }
                File dir = new File(System.getProperty("java.io.tmpdir") + "susteam/sdk/" + gameId);
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
        return promise.future();
    }


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
                    .post("/api/achieveProcess/" + gameId)
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
                    .post("/api/achievement/" + gameId)
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

            client.get("/api/achievement/" + gameId)
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

    public static Future<Achievement> getAchievement(Achievement achievement) {
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

            client.get("/api/achievement/" + gameId + "/" + URLEncoder.encode(achievement.getAchievementName(), StandardCharsets.UTF_8))
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

            client.get("/api/achieveProcess/" + username + "/" + gameId + "/" + URLEncoder.encode(achievementName, StandardCharsets.UTF_8))
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


//    public static void main(String[] args) {
//        SusteamSdk.init("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3QwMDEiLCJwZXJtaXNzaW9ucyI6W10sImlhdCI6MTYwNTcwNzIxNn0.jo9VGmhssPLcKBvU2RfQOGTIsPnd1g-t5LD2ZI-ftqmEBJY06I0a5_kXN1Qc31AoUSwDNEp3JLY0Xku0-faw1DQGOSUUJLKf2wnvzY-36ZoGgVDgZEVgwfKuTyGL-uLuJevV3o4CBpcWx4XdJ0sbogx2oAszV1MR6n7bvSyIjPu368-cdRK4qZ_5Yrk9vfb88D8bH8SGR7AC7JINZam7YnFenk-0DDRDztYaQCgQn356Fz29Lzke3DOXw7gSQm1KPP2MQVJrCkUuZdPckl9PCCN7lj8xm8RM0C0H8B7ozp22qHhztqbcBRW0hXtycSlQ3k-QjdTv5P31_pZGwF7TxQ", 10);
//
//        SusteamSdk.isServerOnline().onComplete(it -> {
//            if (it.succeeded()) {
//                System.out.println("server is online");
//            } else {
//                System.out.println("server is not online");
//            }
//        });
//        SusteamSdk.addAchievement("test","test",1);
//        SusteamSdk.user().onSuccess(it -> {
//            System.out.println(it.getUsername());
//        });
//        SusteamSdk.getGame(10);
//        SusteamSdk.save(new File("C:\\Users\\yinpe\\IdeaProjects\\SUSTeam-SDK\\testfile.txt"))
//                .onComplete(it -> {
//                    System.out.println("success");
//                });
//        SusteamSdk.load("testfile.txt");
//        SusteamSdk.getAllGameSaveName();
//        SusteamSdk.deleteSave("test001-10");
//        SusteamSdk.getAllGameSaveName();
//        SusteamSdk.addAchievement("小试牛刀","得到20分10次",10);
//        SusteamSdk.getAllAchievement();
//        Achievement achievement = new Achievement(gameId,1,"小试牛刀","得到20分10次",10);
//        SusteamSdk.getAchievement(achievement);
//        SusteamSdk.getUserAchievementProcess("小试牛刀");
//        SusteamSdk.updateUserAchievementProcess("小试牛刀",10);
//
//    }

}
