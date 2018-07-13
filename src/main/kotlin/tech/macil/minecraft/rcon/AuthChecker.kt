package tech.macil.minecraft.rcon

import org.mindrot.jbcrypt.BCrypt
import java.io.*
import java.util.regex.Pattern
import java.util.stream.Collectors

class AuthChecker(
        entries: Map<String, String>
) {
    companion object {
        private val twoY = Pattern.compile("^\\\$2y\\\$")

        private fun loadFile(file: File): Map<String, String> {
            return BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { bReader ->
                bReader.lines()
                        .map { it.split(':', limit = 2) }
                        .collect(Collectors.toMap({ it[0] }, { it[1] }))
            }
        }
    }

    constructor(file: File) : this(loadFile(file))

    private val map: Map<String, String> = entries.mapValues { (username, hash) ->
        val newHash = twoY.matcher(hash).replaceFirst("\\\$2a\\\$")
        if (!newHash.startsWith("$2a$")) {
            throw RuntimeException("User $username has invalid hash format. Hash must use bcrypt.")
        }
        newHash
    }

    fun check(username: String, password: String): Boolean {
        val expectedHash = map[username] ?: return false
        return BCrypt.checkpw(password, expectedHash)
    }
}
