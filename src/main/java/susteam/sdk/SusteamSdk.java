package susteam.sdk;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;


public class SusteamSdk {

    public static final String SERVER = "susteam.gogo.moe";

    public static Vertx vertx;
    public static WebClient client;
    public static String token;
    public static Game game;

    public static void init(String token, int gameId) {
        vertx = Vertx.vertx();
        client = WebClient.create(
                vertx,
                new WebClientOptions().setDefaultHost(SERVER)
        );
        SusteamSdk.token = token;


        Game(gameId).onSuccess( it -> {
           SusteamSdk.game = it;
        }).onFailure( it -> {

        });
    }

    public static Future<Game> Game(int gameId) {
        Promise<Game> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get("/api/game/"+gameId);
        request.send(result -> {
            if (result.failed()) {
                promise.fail(result.cause());
                return;
            }
            //TODO can't get Chinese character
            if(result.result().bodyAsJsonObject().getBoolean("success")) {
                game = GameKt.toGame(result.result().bodyAsJsonObject().getJsonObject("game"));
                promise.complete(game);
            }
            else {
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

    public static Future<Void> uploadSave(String path) {
        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.post("/api/save").bearerTokenAuthentication(token);
        //TODO api
        try {
            vertx.fileSystem().readFile(path, readResult -> {
                if(readResult.succeeded()) {
                    request.sendBuffer(readResult.result(), result -> {
                        if (result.failed()) {
                            promise.fail(result.cause());
                            return;
                        }
                        promise.complete();
                    });
                }
                else {
                    promise.fail(readResult.cause());
                }
            });
        } catch( Exception e ) {
            promise.fail("File not found");
        }
        return promise.future();
    }



//    public static void main(String[] args) {
//        SusteamSdk.init(
//                "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3QwMDEiLCJwZXJtaXNzaW9ucyI6W10sImlhdCI6MTYwMjE2NTU3M30.fGDVqNazElRUW6SXz11T9jZKAVJMn_7xTM49my7wfa0TY3bcVPD8Zd7mvmyUQKCH04Lj5xr5QD9Z00OWqxYXWHNjMnuXpHVJFQ0s2jL2XoY8CiyCn35JCKMQ20HD5hnSJAs9ACCp7aqSLxPklnBH4A_oD25U3_5MnSiEL7F8mWJIKrF0d5Dr82g47jPCBVyuKz4PNrFJhaiGEhQIRCfktZQ-g03PS02pD2D5GxJr_rcM7LWt_vYY3lwUPIlYj_c4NMwiLaKvGxA0jqFZicdIIGlvJRQ4yR48sWj9dQEj1PKFGgGcDUgVFJQkU4oKqLHwFlzXXUnfaEkXTwE7hAPZkQ",
//                    10
//        );
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
//
//        SusteamSdk.uploadSave("test.txt").onComplete(it -> {
//           if (it.succeeded()) {
//               System.out.println("upload success");
//           } else {
//               System.out.println(it.result());
//           }
//        });
//    }


}
