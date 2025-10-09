package no.nav.dokgen.configuration

import no.nav.familie.log.filter.RequestTimeFilter


class CustomRequestTimeFilter : RequestTimeFilter() {

     override fun shouldNotFilter(uri: String): Boolean {
         return uri.contains("/internal")
                 || uri == "/api/ping"
                 || uri.contains("/swagger")
                 || uri.contains("/actuator")
     }

}