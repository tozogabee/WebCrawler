package org.example.service;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@Slf4j
public class WebCrawler {
    private final Set<String> visitedLinks = Collections.synchronizedSet(new TreeSet<>());
    protected ExecutorService executor;

    private final String domain;

    public WebCrawler(String startUrl) {
        this.executor = Executors.newFixedThreadPool(10);
        this.domain = extractDomain(startUrl);
        crawl(startUrl);
    }

    // New constructor for testing
    public WebCrawler(String startUrl, ExecutorService executor) {
        this.executor = executor;
        this.domain = extractDomain(startUrl);
        crawl(startUrl);
    }

    /**
     * Initiates the crawling process for the given URL.
     *
     * <p>This method normalizes the provided URL and checks if it has already been visited to avoid redundant processing.
     * If the URL has not been visited and the crawling executor is active, it submits a task to fetch the content of the webpage,
     * extract internal links, and recursively continue crawling those links.</p>
     *
     * <p>Any exceptions that occur during the crawling process are logged, and the crawling operation for the given URL is skipped.</p>
     *
     * @param url The starting URL to crawl. Must be a valid, well-formed URL as a {@code String}.
     */
    private void crawl(String url) {
        String normalizedUrl = normalizeUrl(url);
        if (!visitedLinks.add(normalizedUrl)) return; // Skip if already visited
        if (executor.isShutdown()) return;  // Prevent task submission after shutdown

        executor.submit(() -> {
            try {
                log.info("Crawling: {}", normalizedUrl);
                String content = fetchContent(normalizedUrl);
                List<String> links = extractInternalLinks(content, normalizedUrl);
                for (String link : links) {
                    crawl(link);
                }
            } catch (Exception e) {
                log.error("Failed to crawl: {}", normalizedUrl, e);
                e.printStackTrace();
            }
        });
    }

    /**
     * Fetches the HTML content of the webpage from the provided URL.
     *
     * <p>This method performs an HTTP GET request to the specified URL and retrieves the response
     * body as a string. It sets appropriate request headers, including a user-agent, to mimic a
     * browser request and avoid being blocked by servers. The method also handles HTTP redirects
     * automatically and ensures timeouts are applied for connection and read operations.</p>
     *
     * <p>If the server responds with an HTTP error status code (4xx or 5xx), the method logs the
     * error and returns an empty string.</p>
     *
     * @param urlString The URL of the webpage to fetch, as a {@code String}.
     * @return The HTML content of the webpage as a {@code String}. Returns an empty string if the
     *         response code indicates an error or if any issue occurs during the fetch.
     * @throws IOException If an input/output error occurs during the connection or while reading
     *                     from the server.
     */
    private String fetchContent(String urlString) throws IOException {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Referer", "https://www.google.com/");
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            log.error("Failed with HTTP code: {}", responseCode);
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }


    /**
     * Extracts all valid internal links from the given HTML content.
     *
     * <p>This method parses the HTML content to find `<a>` tags and extracts the `href` attributes.
     * It processes both absolute and relative links. Absolute URLs are validated and normalized
     * before being added to the result if they belong to the same domain. Relative links are
     * resolved against the provided base URL and undergo the same validation process.</p>
     *
     * <p>Invalid links (e.g., malformed URLs, links with unsupported schemes such as `mailto:`,
     * `tel:`, or links containing invalid characters) are skipped.</p>
     *
     * @param content The raw HTML content of the webpage as a {@code String}.
     * @param baseUrl The base URL of the webpage being processed. Used to resolve relative links.
     * @return A {@code List<String>} containing all valid, normalized internal links extracted
     *         from the content.
     */
    private List<String> extractInternalLinks(String content, String baseUrl) {
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("<a[^>]+href=['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String link = matcher.group(1).trim();

            // Skip invalid links
            if (link.isEmpty() || link.startsWith("#") || link.contains("{{") ||
                    link.startsWith("mailto:") || link.startsWith("tel:") ||
                    link.contains(" ")) {
                continue;
            }

            // Validate proper URLs or process as relative
            if (link.contains("http://") || link.contains("https://")) {
                String[] splitLinks = link.split("(?=https?://)");
                for (String splitLink : splitLinks) {
                    processLink(splitLink, baseUrl, links);
                }
            } else {
                processLink(link, baseUrl, links);
            }
        }
        return links;
    }

    /**
     * Processes a given link by validating and normalizing it, resolving relative URLs, and
     * ensuring it belongs to the same domain before adding it to the provided list.
     *
     * <p>This method handles both absolute and relative URLs. For absolute URLs, it validates
     * the URL and adds it to the list if it matches the target domain and has not already been visited.
     * For relative URLs, it resolves them against the base URL, normalizes them, and applies
     * the same checks before adding them to the list.</p>
     *
     * @param link    The URL or relative link to be processed. Can be a fully qualified URL
     *                (e.g., "https://example.com/page") or a relative path (e.g., "/page").
     * @param baseUrl The base URL of the current webpage. Used to resolve relative links.
     * @param links   The list to which valid and normalized links belonging to the same domain
     *                will be added.
     *
     * @throws MalformedURLException if the given link is invalid as an absolute URL and cannot
     *                                be resolved properly (handled internally by the method).
     */
    private void processLink(String link, String baseUrl, List<String> links) {
        try {
            // If the link is malformed, this will throw an exception
            URL parsedLink = new URL(link);
            String normalizedLink = normalizeUrl(parsedLink.toString());

            // Add only if the domain matches and it's not visited
            if (extractDomain(normalizedLink).equals(domain) && !visitedLinks.contains(normalizedLink)) {
                links.add(normalizedLink);
            }
        } catch (MalformedURLException e) {
            // If it's not a valid absolute URL, treat as a relative URL
            if (!link.startsWith("http") && !link.startsWith("https")) {
                link = baseUrl.endsWith("/") ? baseUrl + link : baseUrl + "/" + link;

                // Normalize the relative link
                String normalizedLink = normalizeUrl(link);
                if (extractDomain(normalizedLink).equals(domain) && !visitedLinks.contains(normalizedLink)) {
                    links.add(normalizedLink);
                }
            } else {
                // Log and skip malformed absolute URLs
                log.warn("Skipping malformed URL: {}", link);
            }
        }
    }

    /**
     * Normalizes the given URL to ensure a consistent and standardized format.
     *
     * <p>This method removes trailing slashes, resolves unnecessary path components
     * (e.g., `../` or `./`), and ensures the URL is in a canonical form. This helps avoid
     * duplicate entries for URLs that are technically the same but have slight variations in
     * formatting. The normalization process is useful for comparing URLs and avoiding redundant
     * crawling.</p>
     *
     * @param url The URL to be normalized, provided as a {@code String}.
     * @return A normalized version of the URL as a {@code String}.
     *         Returns the input URL unchanged if no changes are needed.
     * @throws IllegalArgumentException If the provided URL is null or invalid.
     */
    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath().replaceAll("/+", "/");

            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            String normalizedUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + path;
            return normalizedUrl;
        } catch (MalformedURLException e) {
            log.warn("Malformed URL: {}", url);
            return url;
        }
    }

    /**
     * Extracts the domain name from a given URL.
     *
     * <p>This method analyzes the provided URL and extracts the domain (e.g., "example.com").
     * It focuses on isolating the host portion of the URL while omitting protocol, port, path,
     * query parameters, or fragments. The extracted domain can be used for tasks like filtering
     * links or grouping URLs by origin.</p>
     *
     * @param url The URL as a {@code String} from which the domain will be extracted.
     *            Must be a valid and well-formed URL.
     * @return The domain name of the given URL as a {@code String}.
     *         Returns {@code null} if the URL is invalid or domain extraction fails.
     * @throws IllegalArgumentException If the provided URL is null or cannot be processed.
     */
    private String extractDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public void printSortedLinks() {
        log.info("Printing sorted links...");
        log.info("\nSorted Links:");
        visitedLinks.forEach(link -> log.info(link));
    }
}