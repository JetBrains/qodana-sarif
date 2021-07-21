package com.jetbrains.qodana.sarif

import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory

class TextUtil {
    companion object {
        private val policy: PolicyFactory = HtmlPolicyBuilder().toFactory()

        fun sanitizeText(text: String): String {
            var result = ""
            try {
                result = StringEscapeUtils.unescapeHtml4(
                    Jsoup.clean(
                        policy.sanitize(
                            StringEscapeUtils.unescapeJson(
                                Jsoup.parse(
                                    StringEscapeUtils.unescapeJson(text)
                                ).text()
                            )
                        ),
                        Whitelist.none()
                    )
                )
            } catch (e: Exception) {
                println("Can't clean text \"$text\" cause: ${e.message}")
            }
            return result
        }
    }
}