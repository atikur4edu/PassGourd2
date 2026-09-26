package com.example.util

import com.example.data.model.VaultItem
import com.example.security.CryptoEngine

data class VulnerabilityIssue(
    val item: VaultItem,
    val issueType: IssueType,
    val description: String
)

enum class IssueType {
    WEAK,
    REUSED,
    OLD,
    NO_BIOMETRIC
}

data class AuditReport(
    val totalCount: Int,
    val healthyCount: Int,
    val weakCount: Int,
    val reusedCount: Int,
    val overallScore: Int, // 0 to 100
    val issues: List<VulnerabilityIssue>
)

object SecurityAuditor {

    fun generateReport(items: List<VaultItem>): AuditReport {
        if (items.isEmpty()) {
            return AuditReport(0, 0, 0, 0, 100, emptyList())
        }

        val decryptedMap = items.associateWith { item ->
            CryptoEngine.decrypt(item.encryptedPassword)
        }

        val issues = mutableListOf<VulnerabilityIssue>()

        // Check weak passwords
        items.forEach { item ->
            val plain = decryptedMap[item] ?: ""
            val strength = PasswordGenerator.evaluateStrength(plain)
            if (strength.score < 60) {
                issues.add(
                    VulnerabilityIssue(
                        item = item,
                        issueType = IssueType.WEAK,
                        description = "Password is ${strength.label.lowercase()} (${plain.length} chars). Vulnerable to brute force."
                    )
                )
            }
        }

        // Check reused passwords
        val passwordOccurrences = mutableMapOf<String, MutableList<VaultItem>>()
        items.forEach { item ->
            val plain = decryptedMap[item] ?: ""
            if (plain.isNotEmpty()) {
                passwordOccurrences.getOrPut(plain) { mutableListOf() }.add(item)
            }
        }

        passwordOccurrences.values.filter { it.size > 1 }.forEach { sharedGroup ->
            val titles = sharedGroup.map { it.title }.joinToString(", ")
            sharedGroup.forEach { item ->
                issues.add(
                    VulnerabilityIssue(
                        item = item,
                        issueType = IssueType.REUSED,
                        description = "Shared with ${sharedGroup.size - 1} other account(s): $titles"
                    )
                )
            }
        }

        val weakCount = issues.count { it.issueType == IssueType.WEAK }
        val reusedCount = issues.count { it.issueType == IssueType.REUSED }
        val vulnerableUniqueItems = issues.map { it.item.id }.distinct().size
        val healthyCount = (items.size - vulnerableUniqueItems).coerceAtLeast(0)

        val deductions = (weakCount * 12) + (reusedCount * 18)
        val score = (100 - deductions).coerceIn(10, 100)

        return AuditReport(
            totalCount = items.size,
            healthyCount = healthyCount,
            weakCount = weakCount,
            reusedCount = reusedCount,
            overallScore = score,
            issues = issues
        )
    }
}
