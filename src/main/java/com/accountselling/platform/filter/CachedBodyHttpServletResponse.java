package com.accountselling.platform.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper class to cache HTTP response body for logging purposes. Allows capturing the response
 * body while still sending it to the client.
 */
public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

  private final ByteArrayOutputStream cachedBody = new ByteArrayOutputStream();
  private final ServletOutputStream outputStream;
  private final PrintWriter writer;

  public CachedBodyHttpServletResponse(HttpServletResponse response) throws IOException {
    super(response);
    this.outputStream = new CachedBodyServletOutputStream(response.getOutputStream(), cachedBody);
    this.writer = new PrintWriter(new OutputStreamWriter(cachedBody, StandardCharsets.UTF_8));
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return writer;
  }

  public String getBody() {
    return cachedBody.toString(StandardCharsets.UTF_8);
  }

  public byte[] getBodyBytes() {
    return cachedBody.toByteArray();
  }

  private static class CachedBodyServletOutputStream extends ServletOutputStream {

    private final ServletOutputStream originalOutputStream;
    private final ByteArrayOutputStream cachedBody;

    public CachedBodyServletOutputStream(
        ServletOutputStream originalOutputStream, ByteArrayOutputStream cachedBody) {
      this.originalOutputStream = originalOutputStream;
      this.cachedBody = cachedBody;
    }

    @Override
    public boolean isReady() {
      return originalOutputStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      originalOutputStream.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
      originalOutputStream.write(b);
      cachedBody.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      originalOutputStream.write(b);
      cachedBody.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      originalOutputStream.write(b, off, len);
      cachedBody.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      originalOutputStream.flush();
    }

    @Override
    public void close() throws IOException {
      originalOutputStream.close();
    }
  }
}
