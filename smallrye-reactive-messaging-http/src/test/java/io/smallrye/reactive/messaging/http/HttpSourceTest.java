package io.smallrye.reactive.messaging.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class HttpSourceTest extends HttpTestBase {

    @Before
    public void setup() {
        RestAssured.reset();
        addClasses(Receiver.class);
        addConfig(new HttpConnectorConfig("sink", "incoming", null));
        initialize();

        await()
                .catchUncaughtExceptions()
                .until(() -> {
                    Response response = RestAssured.get("/health").andReturn();
                    return response.statusCode() == 200;
                });
    }

    @Test
    public void testWithGet() {
        Response response = RestAssured
                .given()
                .queryParam("foo", "bar")
                .queryParam("baz", 2, 3)
                .get("/")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        response = RestAssured
                .given()
                .queryParam("foo", "bar")
                .queryParam("baz", 2, 3)
                .get("/foo")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        response = RestAssured
                .given()
                .queryParam("foo", "bar")
                .queryParam("baz", 2, 3)
                .get("/bar")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        response = RestAssured
                .given()
                .queryParam("foo", "bar")
                .queryParam("baz", 2, 3)
                .get("/baz")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        List<HttpMessage<?>> list = get(Receiver.class).list();
        assertThat(list).hasSize(4).allMatch(Objects::nonNull);
        assertThat(list.get(0).getPayload()).isInstanceOf(byte[].class).matches(b -> Array.getLength(b) == 0);
        assertThat(list.get(1).getPayload()).isInstanceOf(byte[].class).matches(b -> Array.getLength(b) == 0);
        assertThat(list.get(2).getPayload()).isInstanceOf(byte[].class).matches(b -> Array.getLength(b) == 0);
        assertThat(list.get(3).getPayload()).isInstanceOf(byte[].class).matches(b -> Array.getLength(b) == 0);

        assertThat((list.get(0)).getQuery()).hasSize(2).containsKeys("foo", "baz");
        assertThat(list.get(0).getUrl()).isEqualTo("/");
        assertThat(list.get(1).getUrl()).isEqualTo("/foo");
        assertThat(list.get(2).getUrl()).isEqualTo("/bar");
        assertThat(list.get(3).getUrl()).isEqualTo("/baz");
    }

    @Test
    public void testWithPost() {
        Response response = RestAssured
                .given()
                .body("hello")
                .post("/message")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        response = RestAssured
                .given()
                .body(new JsonObject().put("foo", "bar").encode())
                .post("/json")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        List<HttpMessage<?>> list = get(Receiver.class).list();
        assertThat(list).hasSize(2).allMatch(Objects::nonNull);
        assertThat(list.get(0).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("hello"));
        assertThat(list.get(1).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("{\"foo\":\"bar\"}"));
    }

    @Test
    public void testWithPut() {
        Response response = RestAssured
                .given()
                .body("hello")
                .put("/message2")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        response = RestAssured
                .given()
                .body(new JsonObject().put("foo", "bar").encode())
                .put("/json2")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);

        List<HttpMessage<?>> list = get(Receiver.class).list();
        assertThat(list).hasSize(2).allMatch(Objects::nonNull);
        assertThat(list.get(0).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("hello"));
        assertThat(list.get(1).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("{\"foo\":\"bar\"}"));
    }

    @Test
    public void testWithHeaders() {
        Response response = RestAssured
                .given()
                .header("X-test", "Value")
                .header("Content-Type", "text/plain")
                .body("hello")
                .post("/message")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);
        response = RestAssured
                .given()
                .body(new JsonObject().put("foo", "bar").encode())
                .header("X-test", "Value")
                .header("Content-Type", "application/json")
                .post("/json")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(202);
        List<HttpMessage<?>> list = get(Receiver.class).list();
        assertThat(list).hasSize(2).allMatch(Objects::nonNull);

        assertThat(list.get(0).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("hello"));
        assertThat(list.get(1).getPayload()).isInstanceOf(byte[].class)
                .matches(b -> new String((byte[]) b).equalsIgnoreCase("{\"foo\":\"bar\"}"));
        assertThat(list.get(0).getHeaders()).containsKeys("X-test", "Content-Type");
        assertThat(list.get(1).getHeaders()).containsKeys("X-test", "Content-Type");
    }

    @ApplicationScoped
    public static class Receiver {

        final List<HttpMessage<?>> list = new ArrayList<>();

        @Incoming("sink")
        public CompletionStage<Void> receive(Message<?> m) {
            list.add((HttpMessage<?>) m);
            return m.ack();
        }

        public List<HttpMessage<?>> list() {
            return list;
        }
    }

}
