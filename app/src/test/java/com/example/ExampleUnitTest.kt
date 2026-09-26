package com.example

import com.example.util.PasswordGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPasswordGeneratorLength() {
        val pass = PasswordGenerator.generate(length = 20)
        assertEquals(20, pass.length)
    }

    @Test
    fun testPasswordGeneratorAvoidAmbiguous() {
        val ambiguous = setOf('l', '1', 'I', 'o', 'O', '0')
        repeat(10) {
            val pass = PasswordGenerator.generate(length = 32, avoidAmbiguous = true)
            for (c in pass) {
                assertFalse("Password should not contain ambiguous character $c", c in ambiguous)
            }
        }
    }

    @Test
    fun testPassphraseGenerator() {
        val phrase = PasswordGenerator.generatePassphrase(wordCount = 5, includeNumber = true)
        val parts = phrase.split("-")
        // 5 words + 1 number
        assertEquals(6, parts.size)
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val weak = PasswordGenerator.evaluateStrength("123456")
        assertTrue(weak.score < 40)
        assertEquals("Very Weak", weak.label)

        val strong = PasswordGenerator.evaluateStrength("T9#mK@1!zP92\$vXqL")
        assertTrue(strong.score >= 80)
    }
}
