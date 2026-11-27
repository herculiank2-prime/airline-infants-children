package com.airline.travel.core.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class HtmlCacheHeadersFilterTest {

    private static final String PAGE = "/content/airline/au/en/travel-information/flying-infants-children";

    private final AemContext context = AppAemContext.newAemContext();
    private HtmlCacheHeadersFilter filter;

    @BeforeEach
    void setUp() {
        Map<String, Object> config = new HashMap<>();
        config.put("path.pattern", "^/content/airline/.*");
        filter = context.registerInjectActivateService(new HtmlCacheHeadersFilter(), config);
    }

    private MockSlingHttpServletResponse run(String resourcePath, FilterChain chain) throws Exception {
        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setResourcePath(resourcePath);
        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setExtension("html");
        filter.doFilter(context.request(), context.response(), chain);
        return context.response();
    }

    private static FilterChain writing(int status) {
        return (ServletRequest req, ServletResponse res) -> {
            ((javax.servlet.http.HttpServletResponse) res).setStatus(status);
            res.getWriter().write("<html></html>");
        };
    }

    @Test
    void addsBudgetHeadersToPageHtml() throws Exception {
        MockSlingHttpServletResponse response = run(PAGE, writing(200));

        assertEquals("max-age=60", response.getHeader("Cache-Control"));
        assertEquals("max-age=150, stale-while-revalidate=30, stale-if-error=86400",
                response.getHeader("Surrogate-Control"));
        assertTrue(response.getHeader("Surrogate-Key").matches("airline-page-[0-9a-f]{12}"));
    }

    @Test
    void surrogateKeyIsStablePerPageAndIgnoresJcrContent() {
        assertEquals(HtmlCacheHeadersFilter.surrogateKey(PAGE),
                HtmlCacheHeadersFilter.surrogateKey(HtmlCacheHeadersFilter.pagePath(PAGE + "/jcr:content/root")));
        assertNotEquals(HtmlCacheHeadersFilter.surrogateKey(PAGE),
                HtmlCacheHeadersFilter.surrogateKey("/content/airline/au/en"));
    }

    @Test
    void leavesNonOkResponsesAlone() throws Exception {
        MockSlingHttpServletResponse response = run(PAGE, writing(404));

        assertNull(response.getHeader("Cache-Control"));
        assertNull(response.getHeader("Surrogate-Key"));
    }

    @Test
    void doesNotOverrideCacheControlSetByAem() throws Exception {
        FilterChain chain = (req, res) -> {
            ((javax.servlet.http.HttpServletResponse) res).setHeader("Cache-Control", "no-store");
            res.getOutputStream().write(1);
        };
        MockSlingHttpServletResponse response = run(PAGE, chain);

        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertTrue(response.containsHeader("Surrogate-Key"));
    }

    @Test
    void appliesOnFlushWithoutBody() throws Exception {
        MockSlingHttpServletResponse response = run(PAGE, (req, res) -> res.flushBuffer());

        assertEquals("max-age=60", response.getHeader("Cache-Control"));
    }

    @Test
    void skipsPathsOutsideTheSite() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        run("/content/other-site/page", chain);

        verify(chain).doFilter(context.request(), context.response());
        assertFalse(context.response().containsHeader("Cache-Control"));
    }

    @Test
    void passesNonSlingRequestsThrough() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void clampsNegativeConfigurationToZero() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("max.age", -5);
        HtmlCacheHeadersFilter clamped = context.registerInjectActivateService(new HtmlCacheHeadersFilter(), config);
        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setResourcePath(PAGE);
        clamped.doFilter(context.request(), context.response(), writing(200));

        assertEquals("max-age=0", context.response().getHeader("Cache-Control"));
        clamped.init(null);
        clamped.destroy();
    }
}
