package com.example.util

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

data class PasswordStrength(
    val score: Int, // 0 to 100
    val label: String,
    val colorHex: Long,
    val crackTimeEstimate: String,
    val entropyBits: Double
)

object PasswordGenerator {
    private val secureRandom = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val NUMBERS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private val AMBIGUOUS_CHARS = setOf('l', '1', 'I', 'o', 'O', '0')

    private val MEMORABLE_WORDS = listOf(
        "anchor", "beacon", "castle", "dragon", "falcon", "glacier", "harbor",
        "island", "jungle", "knight", "meteor", "nebula", "orbit", "planet",
        "quartz", "river", "shadow", "timber", "ultra", "velvet", "wizard",
        "zenith", "crystal", "shield", "matrix", "cipher", "aurora", "breeze",
        "copper", "delta", "ember", "frost", "galaxy", "haven", "ignite",
        "jaguar", "kinetic", "lunar", "magnet", "nexus", "oasis", "plasma"
    )

    fun generate(
        length: Int = 16,
        useUpper: Boolean = true,
        useLower: Boolean = true,
        useNumbers: Boolean = true,
        useSymbols: Boolean = true,
        avoidAmbiguous: Boolean = false
    ): String {
        var pool = ""
        if (useLower) pool += LOWERCASE
        if (useUpper) pool += UPPERCASE
        if (useNumbers) pool += NUMBERS
        if (useSymbols) pool += SYMBOLS

        if (pool.isEmpty()) pool = LOWERCASE + NUMBERS

        if (avoidAmbiguous) {
            pool = pool.filter { it !in AMBIGUOUS_CHARS }
        }

        // Guarantee at least one character from each selected class
        val mandatoryChars = mutableListOf<Char>()
        if (useLower) mandatoryChars.add(getRandomChar(LOWERCASE, avoidAmbiguous))
        if (useUpper) mandatoryChars.add(getRandomChar(UPPERCASE, avoidAmbiguous))
        if (useNumbers) mandatoryChars.add(getRandomChar(NUMBERS, avoidAmbiguous))
        if (useSymbols) mandatoryChars.add(getRandomChar(SYMBOLS, false))

        val resultChars = mutableListOf<Char>()
        resultChars.addAll(mandatoryChars)

        while (resultChars.size < length) {
            val randomChar = pool[secureRandom.nextInt(pool.length)]
            resultChars.add(randomChar)
        }

        resultChars.shuffle(secureRandom)
        return resultChars.joinToString("")
    }

    private fun getRandomChar(pool: String, avoidAmbiguous: Boolean): Char {
        val filtered = if (avoidAmbiguous) pool.filter { it !in AMBIGUOUS_CHARS } else pool
        return filtered[secureRandom.nextInt(filtered.length)]
    }

    fun generatePassphrase(wordCount: Int = 4, includeNumber: Boolean = true, capitalize: Boolean = true): String {
        val selectedWords = (1..wordCount).map {
            val word = MEMORABLE_WORDS[secureRandom.nextInt(MEMORABLE_WORDS.size)]
            if (capitalize) word.replaceFirstChar { it.uppercase() } else word
        }.toMutableList()

        if (includeNumber) {
            val num = secureRandom.nextInt(90) + 10
            selectedWords.add(num.toString())
        }

        return selectedWords.joinToString("-")
    }

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(0, "Empty", 0xFF9E9E9E, "Instant", 0.0)
        }

        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) poolSize = 26

        val entropy = password.length * log2(poolSize.toDouble())

        // Calculate score
        val lengthScore = (password.length.coerceAtMost(24) / 24.0) * 40
        val varietyScore = (poolSize / 94.0) * 35
        val entropyBonus = (entropy.coerceAtMost(100.0) / 100.0) * 25
        val rawScore = (lengthScore + varietyScore + entropyBonus).toInt().coerceIn(5, 100)

        val (label, colorHex, crackTime) = when {
            entropy < 30 || password.length < 8 -> Triple("Very Weak", 0xFFEF4444, "Instant")
            entropy < 50 || password.length < 10 -> Triple("Weak", 0xFFF97316, "A few minutes")
            entropy < 70 -> Triple("Fair", 0xFFEAB308, "Several months")
            entropy < 85 -> Triple("Strong", 0xFF10B981, "Centuries")
            else -> Triple("Very Strong", 0xFF059669, "Millions of years")
        }

        return PasswordStrength(
            score = rawScore,
            label = label,
            colorHex = colorHex,
            crackTimeEstimate = crackTime,
            entropyBits = ((entropy * 10).toInt() / 10.0)
        )
    }
}
