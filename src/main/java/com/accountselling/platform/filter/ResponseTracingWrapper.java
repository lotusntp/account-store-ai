package com.accountselling.platform.filter;

import com.accountselling.platform.util.TracingContextCapture;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * A custom HttpServletResponseWrapper that captures tracing context when response is committed.
 * This provides an additional safety net for tracing context preservation by detecting the exact
 * moment when the response is being sent to the client.
 *
 * <p>This wrapper intercepts key response methods (flushBuffer, getWriter, getOutputStream) to
 * detect response commit events and capture tracing context at that moment. This serves as a backup
 * mechanism in case the primary TracingAwareFilterChain approach doesn't capture the context
 * properly.
 */
@Slf4j
public class ResponseTracingWrapper extends HttpServletResponseWrapper {

  private final Consumer<TracingContextCapture> onResponseCommit;
  private final AtomicBoolean contextCaptured = new AtomicBoolean(false);
  private ServletOutputStream wrappedOutputStream;
  private PrintWriter wrappedWriter;

  /**
   * Creates a new ResponseTracingWrapper
   *
   * @param response The original HttpServletResponse to wrap
   * @param onResponseCommit Callback to execute when response commit is detected
   */
  public ResponseTracingWrapper(
      HttpServletResponse response, Consumer<TracingContextCapture> onResponseCommit) {
    super(response);
    this.onResponseCommit = onResponseCommit;

    if (onResponseCommit == null) {
      throw new IllegalArgumentException("onResponseCommit callback cannot be null");
    }

    // ResponseTracingWrapper created successfully
  }

  /** Intercepts flushBuffer to detect response commit */
  @Override
  public void flushBuffer() throws IOException {
    // Capture context before flushing (response is about to be committed)
    captureContextOnCommit("flushBuffer");

    // Call original flushBuffer
    super.flushBuffer();
  }

  /** Intercepts getWriter to detect when response writing begins */
  @Override
  public PrintWriter getWriter() throws IOException {
    if (wrappedWriter == null) {
      PrintWriter originalWriter = super.getWriter();
      wrappedWriter = new TracingPrintWriter(originalWriter);
    }

    return wrappedWriter;
  }

  /** Intercepts getOutputStream to detect when response writing begins */
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (wrappedOutputStream == null) {
      ServletOutputStream originalStream = super.getOutputStream();
      wrappedOutputStream = new TracingServletOutputStream(originalStream);
    }

    return wrappedOutputStream;
  }

  /** Override setStatus to detect response preparation */
  @Override
  public void setStatus(int sc) {
    super.setStatus(sc);

    // Capture context when status is set (response is being prepared)
    captureContextOnCommit("setStatus");
  }

  /** Override sendError to detect error responses */
  @Override
  public void sendError(int sc) throws IOException {
    // Capture context before sending error
    captureContextOnCommit("sendError");

    super.sendError(sc);
  }

  /** Override sendError with message to detect error responses */
  @Override
  public void sendError(int sc, String msg) throws IOException {
    // Capture context before sending error
    captureContextOnCommit("sendError");

    super.sendError(sc, msg);
  }

  /** Override sendRedirect to detect redirect responses */
  @Override
  public void sendRedirect(String location) throws IOException {
    // Capture context before sending redirect
    captureContextOnCommit("sendRedirect");

    super.sendRedirect(location);
  }

  /**
   * Captures tracing context when response commit is detected Uses AtomicBoolean to ensure context
   * is captured only once
   */
  private void captureContextOnCommit(String trigger) {
    if (contextCaptured.compareAndSet(false, true)) {
      try {
        TracingContextCapture context = TracingContextCapture.capture();

        // Notify the callback
        onResponseCommit.accept(context);

      } catch (Exception e) {
        log.warn("Failed to capture tracing context on response commit (trigger: {})", trigger, e);
        // Don't re-throw - we don't want to break the response flow
      }
    }
    // Context already captured, skip silently
  }

  /** Checks if tracing context has been captured */
  public boolean isContextCaptured() {
    return contextCaptured.get();
  }

  /** Custom PrintWriter that detects when writing begins */
  private class TracingPrintWriter extends PrintWriter {
    private final AtomicBoolean firstWrite = new AtomicBoolean(true);

    public TracingPrintWriter(PrintWriter original) {
      super(original);
    }

    @Override
    public void write(int c) {
      captureOnFirstWrite();
      super.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
      captureOnFirstWrite();
      super.write(buf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
      captureOnFirstWrite();
      super.write(s, off, len);
    }

    @Override
    public void print(String s) {
      captureOnFirstWrite();
      super.print(s);
    }

    @Override
    public void println(String s) {
      captureOnFirstWrite();
      super.println(s);
    }

    @Override
    public void flush() {
      captureOnFirstWrite();
      super.flush();
    }

    private void captureOnFirstWrite() {
      if (firstWrite.compareAndSet(true, false)) {
        captureContextOnCommit("PrintWriter.write");
      }
    }
  }

  /** Custom ServletOutputStream that detects when writing begins */
  private class TracingServletOutputStream extends ServletOutputStream {
    private final ServletOutputStream original;
    private final AtomicBoolean firstWrite = new AtomicBoolean(true);

    public TracingServletOutputStream(ServletOutputStream original) {
      this.original = original;
    }

    @Override
    public void write(int b) throws IOException {
      captureOnFirstWrite();
      original.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      captureOnFirstWrite();
      original.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      captureOnFirstWrite();
      original.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      captureOnFirstWrite();
      original.flush();
    }

    @Override
    public void close() throws IOException {
      captureOnFirstWrite();
      original.close();
    }

    @Override
    public boolean isReady() {
      return original.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      original.setWriteListener(writeListener);
    }

    private void captureOnFirstWrite() {
      if (firstWrite.compareAndSet(true, false)) {
        captureContextOnCommit("ServletOutputStream.write");
      }
    }
  }

  /** Creates a string representation for debugging */
  @Override
  public String toString() {
    return String.format(
        "ResponseTracingWrapper{contextCaptured=%s, wrappedResponse=%s}",
        contextCaptured.get(), getResponse().getClass().getSimpleName());
  }
}
