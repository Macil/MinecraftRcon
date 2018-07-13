package tech.macil.minecraft.rcon

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthCheckerTest {
    @Test
    fun check() {
        val checker = AuthChecker(mapOf(
                "user" to "\$2y\$05\$iRoy19e5k5wcdetT7P1Ly.9CCOiDqmCeeJ3mSqtWVF.PjiPFePkXu"
        ))
        assertTrue(checker.check("user", "mypass"))
        assertFalse(checker.check("user1", "mypass"))
        assertFalse(checker.check("user", "mypass1"))
    }

    @Test
    fun verify() {
        assertThrows<RuntimeException> {
            AuthChecker(mapOf(
                    "user" to "invalid"
            ))
        }
    }
}