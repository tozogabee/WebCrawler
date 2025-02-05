# WebCrawler

## Overview

The **WebCrawler** is a multi-threaded Java application designed to recursively crawl web pages, extract internal links, and store them in a sorted manner. It uses `HttpURLConnection` to fetch web pages and `ExecutorService` for concurrent crawling, ensuring efficient handling of large websites.

## Features

- **Multi-threaded crawling**: Uses a thread pool for concurrent requests.
- **Internal link extraction**: Extracts links from `<a>` tags while avoiding external links.
- **Duplicate prevention**: Ensures each URL is processed only once.
- **Graceful shutdown**: Allows safe termination of the crawling process.
- **URL normalization**: Removes trailing slashes and invalid characters.

## Requirements

- Java 11 or later
- Maven (for dependency management and build automation)
- Internet connection (for crawling web pages)

## Installation

1. Clone the repository:
   ```sh
   git clone git@github.com:your-username/WebCrawler.git
   cd WebCrawler
   ```
2. Build the project using Maven:
   ```sh
   mvn clean install
   ```

## Usage

Run the crawler with a starting URL:

```sh
java -jar target/webcrawler.jar https://example.com
```

### Example Output:

```
Crawling: https://example.com
Crawling: https://example.com/about
Crawling: https://example.com/contact
...
Sorted Links:
https://example.com
https://example.com/about
https://example.com/contact
```

##

## Testing

Run unit tests with:

```sh
mvn test
```

Tests use **Mockito** to mock dependencies and verify behavior, ensuring reliability.

## Troubleshooting

- **No links found?**
  - Check if the target website uses JavaScript to load content (try Selenium if needed).
  - Ensure `robots.txt` allows crawling.

## License

This project is licensed under the **MIT License**. Feel free to modify and distribute it.

##

## Author

\*\*Gabor Toth \*\*- [GitHub](https://github.com/tozogabee)



