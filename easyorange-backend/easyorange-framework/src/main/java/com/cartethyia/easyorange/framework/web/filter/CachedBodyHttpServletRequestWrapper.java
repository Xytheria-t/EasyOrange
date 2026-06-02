package com.cartethyia.easyorange.framework.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体的 HttpServletRequest 包装器
 * <p>
 * 将请求体一次性读入 byte[]，后续 {@link #getInputStream()} 和
 * {@link #getReader()} 均从缓存读取，支持多次读取。
 * 用于限流 Filter 需要在过滤阶段读取请求体（计算 hash），
 * 同时不破坏后续 Controller 对请求体的正常读取。
 * </p>
 */
public class CachedBodyHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        try (ServletInputStream inputStream = request.getInputStream()) {
            this.cachedBody = inputStream.readAllBytes();
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream byteArrayInputStream;

        CachedBodyServletInputStream(byte[] cachedBody) {
            this.byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return byteArrayInputStream.read();
        }
    }
}
