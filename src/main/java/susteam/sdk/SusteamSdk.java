package susteam.sdk;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;

public class SusteamSdk {

    public static final String SERVER = "http://susteam.gogo.moe/api/";

    public static Vertx vertx;
    public static WebClient client;
    public static String token;

    public static void init(String token) {
        vertx = Vertx.vertx();
        client = WebClient.create(vertx);
        SusteamSdk.token = token;
    }

    public static Future<Void> isServerOnline() {
        Promise<Void> promise = Promise.promise();
        HttpRequest<Buffer> request = client.get(SERVER + "token");
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
        HttpRequest<Buffer> request = client.get(SERVER + "token");
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

//    public static void main(String[] args) {
//        SusteamSdk.init("aaaaa");
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
//    }

}
