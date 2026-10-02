package com.nuvio.app.features.addons

private const val ADDON_URL_HEX = "0123456789ABCDEF"

internal fun String.encodeUnsafeHttpUrlCharacters(): String =
    buildString(length) {
        this@encodeUnsafeHttpUrlCharacters.forEach { char ->
            when (char) {
                ' ' -> append("%20")
                '"' -> append("%22")
                '<' -> append("%3C")
                '>' -> append("%3E")
                '\\' -> append("%5C")
                '^' -> append("%5E")
                '`' -> append("%60")
                '{' -> append("%7B")
                '|' -> append("%7C")
                '}' -> append("%7D")
                else -> {
                    if (char.code <= 0x1F || char.code == 0x7F) {
                        char.toString().encodeToByteArray().forEach { byte ->
                            val value = byte.toInt() and 0xFF
                            append('%')
                            append(ADDON_URL_HEX[value shr 4])
                            append(ADDON_URL_HEX[value and 0x0F])
                        }
                    } else {
                        append(char)
                    }
                }
            }
        }
    }
