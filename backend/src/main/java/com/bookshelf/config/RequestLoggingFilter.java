package com.bookshelf.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String client = request.getRemoteAddr();
        String fullUri = StringUtils.hasText(query) ? uri + "?" + query : uri;
        boolean hasAuth = StringUtils.hasText(request.getHeader("Authorization"));

        log.info(">>> {} {} [client={}, auth={}]", method, fullUri, client, hasAuth ? "Bearer" : "none");

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("<<< {} {} [status={}, {}ms]", method, uri, status, ms);
            } else if (status >= 400) {
                log.warn("<<< {} {} [status={}, {}ms]", method, uri, status, ms);
            } else {
                log.info("<<< {} {} [status={}, {}ms]", method, uri, status, ms);
            }
        }
    }
}
