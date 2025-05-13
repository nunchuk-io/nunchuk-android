package com.nunchuk.android.core.miniscript

object MiniscriptUtil {
    fun formatMiniscriptCorrectly(input: String, indent: String = "  ", level: Int = 0): String {
        fun isTopLevelFunction(s: String) = !s.contains(',')

        val builder = StringBuilder()
        var i = 0

        while (i < input.length) {
            if (input[i].isLetter()) {
                // Function name
                val start = i
                while (i < input.length && (input[i].isLetter() || input[i] == '_')) i++
                val func = input.substring(start, i)

                // Expect '('
                if (i >= input.length || input[i] != '(') {
                    builder.append(func)
                    continue
                }

                // Find arguments until matching ')'
                var parenCount = 1
                val argsStart = ++i
                while (i < input.length && parenCount > 0) {
                    if (input[i] == '(') parenCount++
                    else if (input[i] == ')') parenCount--
                    i++
                }
                val args = input.substring(argsStart, i - 1)
                val splitArgs = splitArguments(args)

                if (level == 0) {
                    builder.appendLine("$func(")
                    splitArgs.forEachIndexed { index, arg ->
                        val formattedArg = if (arg.contains('(')) {
                            formatMiniscriptCorrectly(arg, indent, level + 1).trim()
                        } else {
                            arg
                        }
                        builder.append(indent).append(formattedArg)
                        if (index != splitArgs.lastIndex) builder.appendLine(",") else builder.appendLine()
                    }
                    builder.append(")")
                } else {
                    // Inline nested calls
                    builder.append("$func(")
                    builder.append(splitArgs.joinToString(", ") { it.trim() })
                    builder.append(")")
                }
            } else {
                builder.append(input[i])
                i++
            }
        }

        return builder.toString()
    }

    fun splitArguments(args: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in args.indices) {
            when (args[i]) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) {
                    result.add(args.substring(start, i).trim())
                    start = i + 1
                }
            }
        }
        result.add(args.substring(start).trim())
        return result
    }

    fun revertFormattedMiniscript(formatted: String): String {
        return formatted
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("") // Concatenate everything into one line
    }
}