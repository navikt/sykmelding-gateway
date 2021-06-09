package no.nav.syfo.sykmelding.gateway.readiness

import no.nav.syfo.sykmelding.gateway.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.exchange
import java.net.URI
import java.time.Duration

const val APPLICATION_READY = "Application is ready!"
const val APPLICATION_NOT_READY = "Application is NOT ready!"

@RestController
class SelfTestController(
    @Value("\${sykmeldingerbackend.url}") private val sykmeldingerBackendUrl: String,
    @Value("\${service.gateway.key}") private val sykmeldingerBackendApiGwKey: String,
) {

    var ready = false
    val log = logger()

    private val restTemplate = RestTemplateBuilder()
        .setReadTimeout(Duration.ofSeconds(2))
        .setConnectTimeout(Duration.ofSeconds(1))
        .build()

    fun syfosoknadErOk(): Boolean {
        val headers = HttpHeaders()
        headers.set("x-nav-apiKey", sykmeldingerBackendApiGwKey)
        val request = RequestEntity<Any>(headers, HttpMethod.GET, URI("$sykmeldingerBackendUrl/is_alive"))
        val res: ResponseEntity<String> = restTemplate.exchange(request)
        return res.statusCode.is2xxSuccessful
    }

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        if (ready) {
            return ResponseEntity.ok(APPLICATION_READY)
        }
        try {

            // if (syfosoknadErOk()) {
            log.info("I am ready")
            ready = true
            return ResponseEntity.ok(APPLICATION_READY)
            // }
            // throw RuntimeException("Ikke klar")
        } catch (e: Exception) {
            log.info("sykmelding-gateway er ikke klar", e)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(APPLICATION_NOT_READY)
        }
    }
}
