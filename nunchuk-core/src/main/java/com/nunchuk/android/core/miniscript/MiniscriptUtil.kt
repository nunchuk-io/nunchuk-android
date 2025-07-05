package com.nunchuk.android.core.miniscript

object MiniscriptUtil {
    
    /**
     * Enhanced Miniscript formatting based on comprehensive formatting rules
     */
    fun formatMiniscript(input: String): String {
        if (input.isEmpty()) return input
        
        // 1. Remove all whitespace characters
        val unformatted = input.unformatMiniscript()
        
        // 2. Add a line break and spaces after the first opening parenthesis/brace
        var formatted = unformatted.replaceFirst("(", "(\n  ")
        formatted = formatted.replaceFirst("{", "{\n  ")
        formatted = formatted.replace(",{", ",\n  {")
        formatted = formatted.replace("{{", "{\n{")
        formatted = formatted.replace("}}", "}\n}")
        formatted = formatted.replace("{(", "{\n(")
        formatted = formatted.replace(")}", ")\n}")
        
        // 3. Add line breaks before operators
        val operators = listOf(
            "older(", "after(",
            "sha256(", "hash256(", "ripemd160(", "hash160(",
            "and_v(", "and_b(", "and_n(", "and(",
            "or_b(", "or_c(", "or_d(", "or_i(", "or(",
            "andor(", "thresh(", "multi(", "multi_a("
        )
        
        for (operator in operators) {
            var searchStartIndex = 0
            while (true) {
                val index = formatted.indexOf(operator, searchStartIndex)
                if (index == -1) break
                
                // Check conditions before adding line break:
                // 1. Not at the very beginning
                // 2. Doesn't already have a line break before it  
                // 3. Has a comma before the operator
                var shouldAddLineBreak = false
                if (index > 0) {
                    val charBefore = formatted[index - 1]
                    if (charBefore != '\n' && charBefore == ',') {
                        shouldAddLineBreak = true
                    }
                }
                
                if (shouldAddLineBreak) {
                    formatted = formatted.substring(0, index) + "\n  " + formatted.substring(index)
                    searchStartIndex = index + operator.length + 3 // 3 for "\n  "
                } else {
                    searchStartIndex = index + operator.length
                }
            }
        }
        
        // 4. Add a line break before the last closing parenthesis
        val lastClosingParenIndex = formatted.lastIndexOf(')')
        if (lastClosingParenIndex != -1) {
            formatted = formatted.substring(0, lastClosingParenIndex) + "\n" + formatted.substring(lastClosingParenIndex)
        }
        
        // 5. Add line breaks for lines longer than 40 characters
        val lines = formatted.split('\n')
        val processedLines = mutableListOf<String>()
        
        for (line in lines) {
            if (line.length <= 40) {
                processedLines.add(line)
            } else {
                // Split long lines at appropriate break points
                var remainingLine = line
                val baseIndent = line.takeWhile { it == ' ' }
                
                while (remainingLine.length > 40) {
                    val maxLength = minOf(40, remainingLine.length)
                    val searchRange = remainingLine.substring(0, maxLength)
                    
                    // Look for break points in order of preference: comma, opening parenthesis
                    var breakPoint: Int? = null
                    
                    // First try to find the last comma within the limit
                    val lastCommaIndex = searchRange.lastIndexOf(',')
                    if (lastCommaIndex != -1) {
                        breakPoint = lastCommaIndex + 1
                    }
                    // If no comma, try the last opening parenthesis
                    else {
                        val lastParenIndex = searchRange.lastIndexOf('(')
                        if (lastParenIndex != -1) {
                            breakPoint = lastParenIndex + 1
                        }
                    }
                    // If no good break point found, break at 40 characters
                    if (breakPoint == null) {
                        breakPoint = maxLength
                    }
                    
                    // Extract the part before the break point
                    val beforeBreak = remainingLine.substring(0, breakPoint)
                    processedLines.add(beforeBreak)
                    
                    // Prepare the remaining part with proper indentation
                    val afterBreak = remainingLine.substring(breakPoint)
                    remainingLine = baseIndent + "  " + afterBreak.trim()
                }
                
                // Add the remaining part if it's not empty
                if (remainingLine.trim().isNotEmpty()) {
                    processedLines.add(remainingLine)
                }
            }
        }
        
        formatted = processedLines.joinToString("\n")
        // 6. Add space after commas for better readability
        formatted = formatted.replace(",", ", ")
        formatted = formatted.replace(",  ", ", ") // Remove double spaces
        formatted = formatted.replace(", \n", ",\n") // Keep line breaks after commas
        
        return formatted
    }
    
    /**
     * Remove all whitespace characters from miniscript
     */
    fun String.unformatMiniscript(): String {
        return this.replace(Regex("\\s+"), "")
    }
    
    /**
     * Legacy formatting method - kept for backward compatibility
     */
    fun formatMiniscriptCorrectly(input: String, indent: String = "  ", level: Int = 0): String {
        return formatMiniscript(input)
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
        return formatted.replace(Regex("\\s+"), "")
    }
}

// Extension functions for String
fun String.formatMiniscript(): String = MiniscriptUtil.formatMiniscript(this)
fun String.unformatMiniscript(): String = MiniscriptUtil.run { this@unformatMiniscript.unformatMiniscript() }