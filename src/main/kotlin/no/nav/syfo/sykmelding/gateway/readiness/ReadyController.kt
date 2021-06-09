package no.nav.syfo.sykmelding.gateway.readiness

import no.nav.syfo.sykmelding.gateway.logger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

const val APPLICATION_READY = "Application is ready!"
const val APPLICATION_NOT_READY = "Application is NOT ready!"

@RestController
class SelfTestController {

    var ready = false
    val log = logger()

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        if (ready) {
            return ResponseEntity.ok(APPLICATION_READY)
        } else {
            log.info("I am ready")
            ready = true
            return ResponseEntity.ok(APPLICATION_READY)
        }
    }
}
