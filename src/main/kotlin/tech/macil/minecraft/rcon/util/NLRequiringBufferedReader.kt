package tech.macil.minecraft.rcon.util

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader

// Just like BufferedReader but only reads lines that ends in '\n'.
class NLRequiringBufferedReader : BufferedReader {
    constructor(r: Reader) : super(r)

    constructor(r: Reader, sz: Int) : super(r, sz)

    override fun readLine(): String? {
        val sb = StringBuilder()
        while (true) {
            val r = read()
            if (r == -1) {
                return null
            }
            if (r == '\n'.toInt()) {
                return sb.toString()
            }
            sb.append(r.toChar())
        }
    }
}
