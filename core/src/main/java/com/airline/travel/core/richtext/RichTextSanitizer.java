package com.airline.travel.core.richtext;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Allow-list for fragment answers (design 3.3). Keeps {@code p ul ol li strong b em i br} without
 * attributes and {@code a} with a safe {@code href} only. Other elements are dropped but their text is
 * kept, except {@code script}, {@code style} and similar, whose content is dropped too. Output is still
 * rendered through HTL's {@code html} context as a second layer.
 */
public final class RichTextSanitizer {

    private static final Set<String> ALLOWED = Set.of("p", "ul", "ol", "li", "strong", "b", "em", "i", "br", "a");
    private static final Set<String> DROP_WITH_CONTENT = Set.of("script", "style", "iframe", "object", "template",
            "noscript", "textarea", "title");
    private static final Pattern COMMENT = Pattern.compile("<!--.*?(-->|$)", Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)([^<>]*)>");
    private static final Pattern HREF = Pattern.compile("(?i)\\bhref\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))");
    private static final Pattern CONTROL = Pattern.compile("[\\u0000-\\u0020\\u007f]");

    private RichTextSanitizer() {
    }

    public static String sanitize(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        String input = COMMENT.matcher(html).replaceAll("");
        StringBuilder out = new StringBuilder(input.length());
        Matcher tag = TAG.matcher(input);
        int position = 0;
        String dropping = null;
        while (tag.find()) {
            String name = tag.group(2).toLowerCase(Locale.ROOT);
            boolean closing = !tag.group(1).isEmpty();
            if (dropping == null) {
                out.append(escapeText(input.substring(position, tag.start())));
            }
            position = tag.end();
            if (dropping != null) {
                if (closing && name.equals(dropping)) {
                    dropping = null;
                }
                continue;
            }
            if (DROP_WITH_CONTENT.contains(name)) {
                if (!closing && !tag.group(3).trim().endsWith("/")) {
                    dropping = name;
                }
                continue;
            }
            if (!ALLOWED.contains(name)) {
                continue;
            }
            if (closing) {
                if (!"br".equals(name)) {
                    out.append("</").append(name).append('>');
                }
            } else if ("a".equals(name)) {
                String href = safeHref(tag.group(3));
                out.append(href == null ? "<a>" : "<a href=\"" + escapeAttribute(href) + "\">");
            } else {
                out.append('<').append(name).append('>');
            }
        }
        if (dropping == null) {
            out.append(escapeText(input.substring(position)));
        }
        return out.toString().trim();
    }

    static String safeHref(String attributes) {
        Matcher matcher = HREF.matcher(attributes);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(2) != null ? matcher.group(2)
                : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
        String value = unescape(raw).trim();
        String probe = CONTROL.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
        boolean allowed = probe.startsWith("https:") || probe.startsWith("mailto:") || probe.startsWith("tel:")
                || probe.startsWith("#") || (probe.startsWith("/") && !probe.startsWith("//"));
        return allowed ? value : null;
    }

    /** Text from the rich text editor is already entity-encoded; only stray angle brackets need escaping. */
    private static String escapeText(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String value) {
        return value.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&colon;", ":").replace("&#58;", ":").replace("&amp;", "&");
    }
}
