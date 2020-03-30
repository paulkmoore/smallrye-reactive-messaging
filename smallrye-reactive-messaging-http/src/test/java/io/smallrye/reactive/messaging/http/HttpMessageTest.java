package io.smallrye.reactive.messaging.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.vertx.mutiny.core.Vertx;

public class HttpMessageTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);
    private Vertx vertx;
    private HttpSink sink;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("url", "http://localhost:8089/items");
        sink = new HttpSink(vertx, new HttpConnectorOutgoingConfiguration(new HttpConnectorConfig("foo", map)));
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testHeadersAndUrlAndQuery() {
        stubFor(post(urlEqualTo("/items"))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching("/record?.*"))
                .willReturn(aResponse()
                        .withStatus(204)));

        String uuid = UUID.randomUUID().toString();
        HttpMessage<String> message = HttpMessage.HttpMessageBuilder.<String> create()
                .withHeader("X-foo", "value")
                .withUrl("http://localhost:8089/record")
                .withQueryParameter("name", "clement")
                .withPayload(uuid)
                .build();

        sink.send(message).subscribeAsCompletionStage();
        awaitForRequest();

        assertThat(bodies("/record?name=clement")).hasSize(1);
        LoggedRequest request = requests("/record?name=clement").get(0);
        assertThat(request.getBodyAsString()).isEqualTo(uuid);
        assertThat(request.getHeader("X-foo")).isEqualTo("value");
        assertThat(request.getMethod().getName()).isEqualToIgnoringCase("POST");
        QueryParameter name = request.getQueryParams().get("name");
        assertThat(name).isNotNull();
        assertThat(name.isSingleValued()).isTrue();
        assertThat(name.firstValue()).isEqualToIgnoringCase("clement");
    }

    @Test
    public void testHeadersAndUrlAndQueryOnRawMessage() {
        stubFor(post(urlEqualTo("/items"))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching("/record?.*"))
                .willReturn(aResponse()
                        .withStatus(204)));

        String uuid = UUID.randomUUID().toString();
        Message<String> message = Message.of(uuid).withMetadata(Metadata.of(
                HttpResponseMetadata.builder()
                        .withUrl("http://localhost:8089/record")
                        .withHeader("X-foo", "value")
                        .withQueryParameter("name", "clement").build()));

        sink.send(message).subscribeAsCompletionStage();
        awaitForRequest();

        assertThat(bodies("/record?name=clement")).hasSize(1);
        LoggedRequest request = requests("/record?name=clement").get(0);
        assertThat(request.getBodyAsString()).isEqualTo(uuid);
        assertThat(request.getHeader("X-foo")).isEqualTo("value");
        assertThat(request.getMethod().getName()).isEqualToIgnoringCase("POST");
        QueryParameter name = request.getQueryParams().get("name");
        assertThat(name).isNotNull();
        assertThat(name.isSingleValued()).isTrue();
        assertThat(name.firstValue()).isEqualToIgnoringCase("clement");
    }

    @Test
    public void testWithDefaultURLWithPut() {
        stubFor(put(urlEqualTo("/items"))
                .willReturn(aResponse()
                        .withStatus(204)));

        stubFor(post(urlEqualTo("/items"))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(post(urlPathMatching("/record?.*"))
                .willReturn(aResponse()
                        .withStatus(404)));

        String uuid = UUID.randomUUID().toString();
        HttpMessage<String> message = HttpMessage.HttpMessageBuilder.<String> create()
                .withHeader("X-foo", "value")
                .withHeader("X-foo", "value-2")
                .withMethod("PUT")
                .withPayload(uuid)
                .build();

        assertThat(message.getMethod()).isEqualTo("PUT");
        assertThat(message.getHeaders()).containsExactly(entry("X-foo", Arrays.asList("value", "value-2")));
        assertThat(message.getPayload()).isEqualTo(uuid);
        assertThat(message.getQuery()).isEmpty();
        assertThat(message.getUrl()).isNull();

        sink.send(message).subscribeAsCompletionStage();
        awaitForRequest();

        assertThat(bodies("/items")).hasSize(1);
        LoggedRequest request = requests("/items").get(0);
        assertThat(request.getBodyAsString()).isEqualTo(uuid);
        assertThat(request.getHeaders().getHeader("X-foo").values()).containsExactly("value", "value-2");
        assertThat(request.getMethod().getName()).isEqualToIgnoringCase("PUT");
    }

    private void awaitForRequest() {
        await().until(() -> wireMockRule.getServeEvents().getRequests().size() >= 1);
    }

    private List<LoggedRequest> requests(String path) {
        return wireMockRule.getServeEvents().getRequests().stream().map(ServeEvent::getRequest)
                .filter(req -> req.getUrl().equalsIgnoreCase(path))
                .collect(Collectors.toList());
    }

    private List<String> bodies(String path) {
        return requests(path).stream().map(LoggedRequest::getBodyAsString).collect(Collectors.toList());
    }

}
