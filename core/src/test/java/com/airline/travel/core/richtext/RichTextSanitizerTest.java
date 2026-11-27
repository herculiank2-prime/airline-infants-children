package com.airline.travel.core.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RichTextSanitizerTest {

    @Test
    void keepsAllowedElementsWithoutAttributes() {
        assertEquals("<p>Hello <strong>there</strong> <em>you</em><br></p><ul><li>One</li></ul>",
                RichTextSanitizer.sanitize("<p class=\"x\" style=\"color:red\">Hello <strong>there</strong> "
                        + "<em onclick=\"x()\">you</em><br/></p><ul><li>One</li></ul>"));
    }

    @Test
    void dropsUnknownElementsButKeepsTheirText() {
        assertEquals("<p>Big text</p>", RichTextSanitizer.sanitize("<div><p><span>Big</span> <h1>text</h1></p></div>"));
    }

    @Test
    void dropsScriptsStylesAndComments() {
        assertEquals("<p>ok</p>", RichTextSanitizer.sanitize(
                "<script>alert(1)</script><style>p{}</style><!-- note --><p>ok</p><iframe src=x></iframe>"));
        assertEquals("<p>a</p>", RichTextSanitizer.sanitize("<p>a</p><script/>"));
    }

    @Test
    void keepsOnlySafeLinks() {
        assertEquals("<a href=\"https://example.com/?a=1&amp;b=2\">x</a>",
                RichTextSanitizer.sanitize("<a href=\"https://example.com/?a=1&amp;b=2\" target=\"_blank\">x</a>"));
        assertEquals("<a href=\"/au/en/help.html\">x</a>", RichTextSanitizer.sanitize("<a href='/au/en/help.html'>x</a>"));
        assertEquals("<a href=\"mailto:help@airline.example\">x</a>",
                RichTextSanitizer.sanitize("<a href=mailto:help@airline.example>x</a>"));
        assertEquals("<a href=\"tel:+611300\">x</a>", RichTextSanitizer.sanitize("<a href=\"tel:+611300\">x</a>"));
        assertEquals("<a href=\"#booking\">x</a>", RichTextSanitizer.sanitize("<a href=\"#booking\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a href=\"javascript:alert(1)\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a href=\"java&#58;script:alert(1)\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a href=\" jav\tascript:alert(1)\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a href=\"//evil.example\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a href=\"http://plain.example\">x</a>"));
        assertEquals("<a>x</a>", RichTextSanitizer.sanitize("<a name=\"top\">x</a>"));
    }

    @Test
    void escapesStrayAngleBrackets() {
        assertEquals("<p>1 &lt; 2 &gt; 0</p>", RichTextSanitizer.sanitize("<p>1 < 2 > 0</p>"));
    }

    @Test
    void blankInputGivesEmptyString() {
        assertEquals("", RichTextSanitizer.sanitize(null));
        assertEquals("", RichTextSanitizer.sanitize("  "));
    }
}
