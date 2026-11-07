package com.example.ses;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exercises the API over real HTTP against a running server. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ses.api-key=test-key-0123456789", "ses.max-body-bytes=2048"})
class EmailApiTest {

    private static final String KEY = "test-key-0123456789";

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JsonMapper json;

    private HttpRequest.Builder request(String path, String key) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        return key == null ? builder : builder.header("Authorization", "Bearer " + key);
    }

    private HttpResponse<String> post(BodyPublisher body) throws Exception {
        return http.send(request("/v1/emails", KEY).header("Content-Type", "application/json").POST(body).build(),
                BodyHandlers.ofString());
    }

    private HttpResponse<String> post(Map<String, Object> body) throws Exception {
        return post(BodyPublishers.ofString(json.writeValueAsString(body)));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(request(path, KEY).GET().build(), BodyHandlers.ofString());
    }

    private static Map<String, Object> validEmail() {
        return Map.of("from", "sender@example.com", "to", List.of("to@example.com"),
                "cc", List.of("cc@example.com"), "bcc", List.of("bcc@example.com"),
                "subject", "Hello", "text", "Plain", "html", "<p>Rich</p>");
    }

    @Test
    void sentEmailCanBeReadBackAndListed() throws Exception {
        HttpResponse<String> sent = post(validEmail());
        assertThat(sent.statusCode()).isEqualTo(201);
        String id = json.readTree(sent.body()).get("id").asString();
        assertThat(sent.headers().firstValue("Location")).contains("/v1/emails/" + id);

        HttpResponse<String> fetched = get("/v1/emails/" + id);
        assertThat(fetched.statusCode()).isEqualTo(200);
        JsonNode email = json.readTree(fetched.body());
        assertThat(email.get("from").asString()).isEqualTo("sender@example.com");
        assertThat(email.get("bcc").get(0).asString()).isEqualTo("bcc@example.com");
        assertThat(email.get("html").asString()).isEqualTo("<p>Rich</p>");

        JsonNode newest = json.readTree(get("/v1/emails?limit=1").body()).get("emails");
        assertThat(newest).hasSize(1);
        assertThat(newest.get(0).get("id").asString()).isEqualTo(id);
        assertThat(newest.get(0).has("text")).isFalse();
        assertThat(newest.get(0).has("bcc")).isFalse();
    }

    @Test
    void invalidEmailReportsEveryProblem() throws Exception {
        HttpResponse<String> response = post(Map.of("from", "nope", "to", List.of("also nope")));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("from:", "to[0]:", "subject:", "text:");
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        assertThat(post(BodyPublishers.ofString("{not json")).statusCode()).isEqualTo(400);
        assertThat(post(BodyPublishers.ofString("{\"to\":\"not-a-list\"}")).statusCode()).isEqualTo(400);
    }

    @Test
    void wrongContentTypeIsRejected() throws Exception {
        HttpRequest request = request("/v1/emails", KEY).header("Content-Type", "text/plain")
                .POST(BodyPublishers.ofString("{}")).build();
        assertApiError(http.send(request, BodyHandlers.ofString()), 415);
    }

    @Test
    void frameworkFailuresUseTheApiErrorShape() throws Exception {
        assertApiError(get("/v1/emails?limit=abc"), 400);
        assertApiError(get("/nope"), 404);
        assertApiError(http.send(request("/v1/emails", KEY).DELETE().build(), BodyHandlers.ofString()), 405);
    }

    private void assertApiError(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("error").isString()).isTrue();
        assertThat(body.get("message").isString()).isTrue();
        assertThat(body.get("details").isArray()).isTrue();
    }

    @Test
    void oversizedBodyIsRejected() throws Exception {
        assertThat(post(BodyPublishers.ofByteArray(new byte[2049])).statusCode()).isEqualTo(413);
    }

    @Test
    void oversizedChunkedBodyIsRejected() throws Exception {
        BodyPublisher chunked = BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(new byte[2049]));
        assertThat(chunked.contentLength()).isEqualTo(-1);
        assertThat(post(chunked).statusCode()).isEqualTo(413);
    }

    @Test
    void unknownEmailIsNotFound() throws Exception {
        assertThat(get("/v1/emails/missing").statusCode()).isEqualTo(404);
    }

    @Test
    void outOfRangeLimitIsRejected() throws Exception {
        assertThat(get("/v1/emails?limit=0").statusCode()).isEqualTo(400);
        assertThat(get("/v1/emails?limit=201").statusCode()).isEqualTo(400);
    }

    @Test
    void missingOrWrongApiKeyIsUnauthorized() throws Exception {
        for (String key : new String[] {null, "wrong-key"}) {
            HttpResponse<String> response =
                    http.send(request("/v1/emails", key).GET().build(), BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        }
    }
}
