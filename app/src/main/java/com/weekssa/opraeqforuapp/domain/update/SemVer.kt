package com.weekssa.opraeqforuapp.domain.update

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prereleaseIdentifiers: List<String> = emptyList(),
    val buildMetadata: String? = null,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
            .takeIf { it != 0 }
            ?.let { return it }

        if (prereleaseIdentifiers.isEmpty() || other.prereleaseIdentifiers.isEmpty()) {
            return when {
                prereleaseIdentifiers.isEmpty() && other.prereleaseIdentifiers.isEmpty() -> 0
                prereleaseIdentifiers.isEmpty() -> 1
                else -> -1
            }
        }

        val sharedIdentifierCount = minOf(prereleaseIdentifiers.size, other.prereleaseIdentifiers.size)
        for (index in 0 until sharedIdentifierCount) {
            val left = prereleaseIdentifiers[index]
            val right = other.prereleaseIdentifiers[index]
            val comparison = comparePrereleaseIdentifier(left, right)
            if (comparison != 0) return comparison
        }

        return prereleaseIdentifiers.size.compareTo(other.prereleaseIdentifiers.size)
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (prereleaseIdentifiers.isNotEmpty()) append('-').append(prereleaseIdentifiers.joinToString("."))
        buildMetadata?.let { append('+').append(it) }
    }

    companion object {
        fun parse(value: String): SemVer? {
            val match = PATTERN.matchEntire(value.trim()) ?: return null
            val prerelease = match.groupValues[4].takeIf(String::isNotEmpty)
                ?.split('.')
                .orEmpty()
            if (prerelease.any { it.length > 1 && it.all(Char::isDigit) && it.startsWith('0') }) {
                return null
            }
            return SemVer(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
                prereleaseIdentifiers = prerelease,
                buildMetadata = match.groupValues[5].takeIf(String::isNotEmpty),
            )
        }

        private fun comparePrereleaseIdentifier(left: String, right: String): Int {
            val leftIsNumeric = left.all(Char::isDigit)
            val rightIsNumeric = right.all(Char::isDigit)
            return when {
                leftIsNumeric && rightIsNumeric -> {
                    left.length.compareTo(right.length).takeIf { it != 0 } ?: left.compareTo(right)
                }
                leftIsNumeric -> -1
                rightIsNumeric -> 1
                else -> left.compareTo(right)
            }
        }

        private val PATTERN = Regex(
            "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
                "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" +
                "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$",
        )
    }
}
