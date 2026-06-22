package com.example.ui.code

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

object CodeHighlighter {

    // Tokenize HTML
    fun highlightHtml(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            
            // Regex for tags: <tag>, </tag>
            val tagRegex = Regex("</?[a-zA-Z0-9:-]+>?|\\s[a-zA-Z0-9:-]+=")
            // Regex for attributes values: "value"
            val attrRegex = Regex("\"[^\"]*\"|'[^']*'")
            // Regex for comments: <!-- -->
            val commentRegex = Regex("<!--[\\s\\S]*?-->")

            // Highlight tags
            tagRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = VsAccentColor, fontWeight = FontWeight.SemiBold), match.range.first, match.range.last + 1)
            }
            
            // Highlight attribute values
            attrRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = VsString), match.range.first, match.range.last + 1)
            }

            // Highlight comments
            commentRegex.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = VsComment, fontFamily = FontFamily.Monospace), match.range.first, match.range.last + 1)
            }
        }
    }

    // Tokenize JS / TS / Kotlin code
    fun highlightCode(code: String, language: String): AnnotatedString {
        if (language.equals("html", ignoreCase = true)) {
            return highlightHtml(code)
        }

        return buildAnnotatedString {
            append(code)

            // Keywords to highlight depending on language
            val keywords = setOf(
                "fun", "val", "var", "class", "import", "package", "return", "if", "else", "for", "while",
                "let", "const", "function", "var", "document", "window", "import", "export", "from",
                "default", "null", "true", "false", "this", "override", "private", "public", "suspend", "interface", "abstract"
            )

            // Highlight keywords
            val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
            wordRegex.findAll(code).forEach { match ->
                if (match.value in keywords) {
                    addStyle(
                        SpanStyle(color = VsKeyword, fontWeight = FontWeight.Bold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // Functions: word followed by parenthesis e.g. function()
            val funRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()")
            funRegex.findAll(code).forEach { match ->
                if (match.value !in keywords) {
                    addStyle(
                        SpanStyle(color = VsFunction),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // Highlight Numbers
            val numRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
            numRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = VsNumber),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Highlight Strings
            val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
            stringRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = VsString),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Highlight Comments
            val lineCommentRegex = Regex("//.*")
            val blockCommentRegex = Regex("/\\*[\\s\\S]*?\\*/")
            
            lineCommentRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = VsComment, fontFamily = FontFamily.Monospace),
                    match.range.first,
                    match.range.last + 1
                )
            }
            blockCommentRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = VsComment, fontFamily = FontFamily.Monospace),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}
