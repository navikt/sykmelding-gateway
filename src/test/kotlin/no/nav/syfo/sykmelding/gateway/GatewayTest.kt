package no.nav.syfo.sykmelding.gateway

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = RANDOM_PORT,
    properties = [
        "sykmeldingerbackend.url=http://localhost:\${wiremock.server.port}/sykmeldinger-backend",
        "service.gateway.key=husnokkel",
    ]
)
@AutoConfigureWireMock(port = 0)
class GatewayTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun testHealth() {
        webClient
            .get().uri("/internal/health")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun testIsReadyErKlar() {
        stubFor(
            get(urlEqualTo("/internal/health"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/internal/isReady")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `ok kall videresendes`() {
        stubFor(
            post(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger/1234/bekreft"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/sykmeldinger-backend/api/v1/sykmeldinger/1234/bekreft")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `ok kall videresendes med path parameter`() {
        stubFor(
            get(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger/1234"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/sykmeldinger-backend/api/v1/sykmeldinger/1234")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `500 kall videresendes`() {
        stubFor(
            get(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/sykmeldinger-backend/api/v1/sykmeldinger")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `ukjent api returnerer 404`() {
        webClient
            .post().uri("/dfgasdyfghuadsfgliuafdg")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `selvbetjening cookie flyttes til auth header`() {
        stubFor(
            get(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger"))
                .withHeader("Authorization", EqualToPattern("Bearer napoleonskake"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/sykmeldinger-backend/api/v1/sykmeldinger")
            .cookie("selvbetjening-idtoken", "napoleonskake")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `cors request`() {
        stubFor(
            post(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger/1234/send"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/sykmeldinger-backend/api/v1/sykmeldinger/1234/send")
            .header("Origin", "http://domain.nav.no")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://domain.nav.no")
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `cors preflight request`() {
        webClient
            .options().uri("/sykmeldinger-backend/api/v1/sykmeldinger/1234/send")
            .header("Origin", "http://domain.nav.no")
            .header("Access-Control-Request-Method", "GET")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://domain.nav.no")
            .expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
            .expectBody().isEmpty
    }

    @Test
    fun `cors request med feil origin returnerer 403`() {
        webClient
            .get().uri("/sykmeldinger-backend/api/v1/sykmeldinger")
            .header("Origin", "http://kompromittertside.com")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `api gw key legges på`() {
        stubFor(
            get(urlEqualTo("/sykmeldinger-backend/api/v1/sykmeldinger"))
                .withHeader("x-nav-apiKey", EqualToPattern("husnokkel"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/sykmeldinger-backend/api/v1/sykmeldinger")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }
}
