package com.jobjam.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PageFetcherTest {

    @Test
    void readableTextKeepsStructureAndDropsPageChrome() {
        String html = """
                <html><head>
                  <script type="application/ld+json">{"@type":"JobPosting","title":"Backend Engineer"}</script>
                  <script>trackVisitor()</script>
                </head><body>
                  <nav>Home · Jobs · Login</nav>
                  <h1>Backend   Engineer</h1>
                  <p>Remote, EU time zones.</p>
                  <ul><li>5+ years of Java</li><li>Spring Boot</li></ul>
                  <footer>© Acme</footer>
                </body></html>""";

        assertThat(PageFetcher.readableText(Jsoup.parse(html))).isEqualTo("""
                Structured data from the page:
                {"@type":"JobPosting","title":"Backend Engineer"}

                ## Backend Engineer
                Remote, EU time zones.
                - 5+ years of Java
                - Spring Boot""");
    }

    @Test
    void readableTextIsCapped() {
        String html = "<p>" + "a".repeat(PageFetcher.MAX_TEXT_CHARS + 100) + "</p>";

        assertThat(PageFetcher.readableText(Jsoup.parse(html))).hasSize(PageFetcher.MAX_TEXT_CHARS);
    }

    @Test
    void refusesLocalAndPrivateAddresses() {
        for (String url : new String[] {"http://localhost:8090/api/jobs", "http://127.0.0.1/", "http://10.0.0.5/",
                "http://192.168.1.1/", "http://169.254.169.254/latest/meta-data/", "http://[::1]/", "http://[fd00::1]/"}) {
            assertThatThrownBy(() -> PageFetcher.publicHttpUri(url))
                    .as(url)
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    void refusesNonHttpSchemes() {
        assertThatThrownBy(() -> PageFetcher.publicHttpUri("file:///etc/passwd"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only http(s)");
    }
}
