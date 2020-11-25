package susteam.sdk;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public class SusteamSdk {

    public static final String SERVER_HOST = "susteam.gogo.moe";

    public static Vertx vertx;
    public static WebClient client;
    public static String token;
    public static int gameId;

    public static void init(String token, int gameId) {
        vertx = Vertx.vertx();
        client = WebClient.create(
                vertx,
                new WebClientOptions().setDefaultHost(SERVER_HOST)
        );
        SusteamSdk.token = token;
        SusteamSdk.gameId = gameId;
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

            client.get("/api/save/"+username+"/"+gameId)
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send( res -> {
                        if (res.succeeded()) {
                            if (res.result().bodyAsJsonObject().getBoolean("success")) {
                                JsonArray saves = res.result().bodyAsJsonObject().getJsonArray("gameSaves");
                                GameSave[] gameSaves = new GameSave[saves.size()];
                                for( int i = 0; i < saves.size(); i++ ) {
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

            client.get("/api/save/"+username+"/"+gameId+"/"+fileName+"/delete")
                    .bearerTokenAuthentication(SusteamSdk.token)
                    .send( res -> {
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
            }
            catch( Exception e ) {
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
                File dir = new File(System.getProperty("java.io.tmpdir")+"susteam/sdk/"+gameId);
                dir.mkdirs();
                File file = new File(dir + "/" + fileName);

                try {
                    file.createNewFile();
                    Files.write(file.toPath(),res.result().bodyAsBuffer().getBytes(), StandardOpenOption.WRITE);
                    promise.complete();
                } catch (IOException e) {
                    e.printStackTrace();
                    promise.fail("file error");
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
//
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
//    }


}
