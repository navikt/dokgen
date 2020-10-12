package no.nav.dokgen

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [DokgenApplication::class])
@ActiveProfiles("dev")
class DokgenTests {
    @Test
    fun contextLoads() {
    }
}