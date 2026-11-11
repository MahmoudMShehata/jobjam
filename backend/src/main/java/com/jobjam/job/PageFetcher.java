package com.jobjam.job;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Downloads a job posting page and reduces it to readable text, for models that cannot browse. */
@Component
public class PageFetcher {

    private static final int MAX_REDIRECTS = 5;
    private static final int TIMEOUT_MILLIS = 15_000;
    private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;
    // Plenty for one posting while leaving room in a small local model's context.
    static final int MAX_TEXT_CHARS = 30_000;
    // Many job sites turn away requests that do not look like a browser.
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";

    public String fetch(String url) {
        Connection.Response response = follow(url);
        Document page;
        try {
            page = response.parse();
        } catch (IOException e) {
            throw unreachable("Could not read the page: " + e.getMessage());
        }
        String text = readableText(page);
        if (text.isBlank()) {
            throw unreachable("The page has no readable text; it may only render with JavaScript.");
        }
        return text;
    }

    /** Fetches {@code url}, following redirects only to public addresses. */
    private Connection.Response follow(String url) {
        URI uri = publicHttpUri(url);
        for (int redirects = 0; ; redirects++) {
            Connection.Response response;
            try {
                response = Jsoup.connect(uri.toString())
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT_MILLIS)
                        .maxBodySize(MAX_BODY_BYTES)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .execute();
            } catch (IOException e) {
                throw unreachable("Could not load " + uri.getHost() + ": " + e.getMessage() + ".");
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400 && response.hasHeader("Location")) {
                if (redirects == MAX_REDIRECTS) {
                    throw unreachable(uri.getHost() + " redirected too many times.");
                }
                uri = publicHttpUri(uri.resolve(response.header("Location")).toString());
                continue;
            }
            if (status >= 400) {
                throw unreachable(uri.getHost() + " answered HTTP " + status
                        + "; the site may block automated access.");
            }
            return response;
        }
    }

    /**
     * Rejects anything but http(s) URLs on public hosts, so a posting link cannot make the
     * server read its own network (localhost, the database, cloud metadata endpoints).
     */
    static URI publicHttpUri(String url) {
        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a valid URL: " + url);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only http(s) links can be fetched");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw unreachable("Could not find " + uri.getHost() + ".");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()
                    || (address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Links to local or private network addresses cannot be fetched");
            }
        }
        return uri;
    }

    /** The page as plain text with one line per block, prefixed by any embedded JobPosting data. */
    static String readableText(Document page) {
        StringBuilder text = new StringBuilder();
        // Job boards often embed a schema.org JobPosting with details the visible page only loads via JavaScript.
        for (Element script : page.select("script[type=application/ld+json]")) {
            if (script.data().contains("JobPosting")) {
                text.append("Structured data from the page:\n").append(script.data().strip()).append("\n\n");
            }
        }

        Element body = page.body();
        body.select("script, style, noscript, template, svg, iframe, nav, footer, form, button").remove();
        body.select("li").forEach(item -> item.prependText("- "));
        body.select("h1, h2, h3, h4, h5, h6").forEach(heading -> heading.prependText("## "));
        body.select("br, p, div, li, tr, ul, ol, dd, dt, table, section, article, header, main, aside,"
                + " h1, h2, h3, h4, h5, h6").forEach(block -> block.after(new TextNode("\n")));

        List<String> lines = new ArrayList<>();
        for (String line : body.wholeText().split("\n")) {
            String trimmed = line.replaceAll("\\s+", " ").strip();
            if (!trimmed.isEmpty() && !trimmed.equals("-") && !trimmed.equals("##")) {
                lines.add(trimmed);
            }
        }
        text.append(String.join("\n", lines));
        return text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text.toString();
    }

    // The UI follows link errors with advice to paste the text or upload screenshots instead.
    private static ResponseStatusException unreachable(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, reason);
    }
}
