package com.airline.travel.core.filters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Emits the HTML freshness budget (ADR-004) on publish so Dispatcher {@code enableTTL} and the CDN
 * read the same values: Dispatcher 60 s + CDN 150 s + SWR 30 s + browser 60 s = 300 s worst case.
 * Only for GET/HEAD .html responses with status 200 under the configured path; headers already set
 * by AEM are left alone. Requires an OSGi configuration, which exists in {@code config.publish} only.
 */
@Component(
        service = Filter.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
            "sling.filter.scope=REQUEST",
            "sling.filter.methods=GET",
            "sling.filter.methods=HEAD",
            "sling.filter.extensions=html",
            "service.ranking:Integer=-700"
        })
@Designate(ocd = HtmlCacheHeadersFilter.Config.class)
public class HtmlCacheHeadersFilter implements Filter {

    static final String CACHE_CONTROL = "Cache-Control";
    static final String SURROGATE_CONTROL = "Surrogate-Control";
    static final String SURROGATE_KEY = "Surrogate-Key";
    static final String KEY_PREFIX = "airline-page-";

    @ObjectClassDefinition(name = "Airline - HTML cache headers filter",
            description = "Cache-Control / Surrogate-Control / Surrogate-Key for published HTML (ADR-004).")
    public @interface Config {
        @AttributeDefinition(name = "Path pattern", description = "Regular expression matched against the resource path.")
        String path_pattern() default "^/content/airline/.*";

        @AttributeDefinition(name = "Browser max-age (s)")
        int max_age() default 60;

        @AttributeDefinition(name = "CDN max-age (s)", description = "Surrogate-Control max-age.")
        int surrogate_max_age() default 150;

        @AttributeDefinition(name = "CDN stale-while-revalidate (s)")
        int stale_while_revalidate() default 30;

        @AttributeDefinition(name = "CDN stale-if-error (s)")
        int stale_if_error() default 86400;
    }

    private Pattern pathPattern;
    private String cacheControl;
    private String surrogateControl;

    @Activate
    protected void activate(Config config) {
        pathPattern = Pattern.compile(config.path_pattern());
        cacheControl = "max-age=" + Math.max(0, config.max_age());
        surrogateControl = "max-age=" + Math.max(0, config.surrogate_max_age())
                + ", stale-while-revalidate=" + Math.max(0, config.stale_while_revalidate())
                + ", stale-if-error=" + Math.max(0, config.stale_if_error());
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to initialise
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof SlingHttpServletRequest) || !(response instanceof SlingHttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        String resourcePath = slingRequest.getRequestPathInfo().getResourcePath();
        if (resourcePath == null || !pathPattern.matcher(resourcePath).matches()) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, new CacheHeadersResponse((SlingHttpServletResponse) response, pagePath(resourcePath)));
    }

    @Override
    public void destroy() {
        // nothing to release
    }

    static String pagePath(String resourcePath) {
        int jcrContent = resourcePath.indexOf("/jcr:content");
        return jcrContent > 0 ? resourcePath.substring(0, jcrContent) : resourcePath;
    }

    static String surrogateKey(String pagePath) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(pagePath.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(KEY_PREFIX);
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    /** Applies the headers just before the first byte is written, when the final status is known. */
    private final class CacheHeadersResponse extends SlingHttpServletResponseWrapper {

        private final String pagePath;
        private boolean applied;

        CacheHeadersResponse(SlingHttpServletResponse wrapped, String pagePath) {
            super(wrapped);
            this.pagePath = pagePath;
        }

        private void applyHeaders() {
            if (applied) {
                return;
            }
            applied = true;
            if (getStatus() != HttpServletResponse.SC_OK || isCommitted()) {
                return;
            }
            if (!containsHeader(CACHE_CONTROL)) {
                setHeader(CACHE_CONTROL, cacheControl);
            }
            if (!containsHeader(SURROGATE_CONTROL)) {
                setHeader(SURROGATE_CONTROL, surrogateControl);
            }
            setHeader(SURROGATE_KEY, surrogateKey(pagePath));
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            applyHeaders();
            return super.getWriter();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            applyHeaders();
            return super.getOutputStream();
        }

        @Override
        public void flushBuffer() throws IOException {
            applyHeaders();
            super.flushBuffer();
        }
    }
}
