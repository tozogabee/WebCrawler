package org.example.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebCrawlerTest {

    @Test
    void testShutdownGracefulTermination() throws InterruptedException {
        // Arrange
        ExecutorService mockExecutor = mock(ExecutorService.class);
        WebCrawler webCrawler = new WebCrawler("http://example.com", mockExecutor);

        when(mockExecutor.isShutdown()).thenReturn(false);
        when(mockExecutor.awaitTermination(10, TimeUnit.MINUTES)).thenReturn(true);

        // Act
        webCrawler.shutdown();

        // Assert
        verify(mockExecutor).shutdown();
        verify(mockExecutor).awaitTermination(10, TimeUnit.MINUTES);
    }

    @Test
    void testShutdownForcedTermination() throws InterruptedException {
        // Arrange
        ExecutorService mockExecutor = mock(ExecutorService.class);
        WebCrawler webCrawler = new WebCrawler("http://example.com", mockExecutor);

        when(mockExecutor.isShutdown()).thenReturn(false);
        when(mockExecutor.awaitTermination(10, TimeUnit.MINUTES)).thenReturn(false);

        // Act
        webCrawler.shutdown();

        // Assert
        verify(mockExecutor).shutdown();
        verify(mockExecutor).awaitTermination(10, TimeUnit.MINUTES);
        verify(mockExecutor).shutdownNow();
    }

    @Test
    void testShutdownInterruptedException() throws InterruptedException {
        // Arrange
        ExecutorService mockExecutor = mock(ExecutorService.class);
        WebCrawler webCrawler = new WebCrawler("http://example.com", mockExecutor);

        when(mockExecutor.isShutdown()).thenReturn(false);
        doThrow(new InterruptedException()).when(mockExecutor).awaitTermination(10, TimeUnit.MINUTES);

        // Act
        webCrawler.shutdown();

        // Assert
        verify(mockExecutor).shutdown();
        verify(mockExecutor).awaitTermination(10, TimeUnit.MINUTES);
        verify(mockExecutor).shutdownNow();
        assertTrue(Thread.interrupted());
    }
}
