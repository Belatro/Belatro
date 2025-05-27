package backend.belatro.components;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Very light per-IP limiter: max 20 concurrent handshakes.
 * Replace with Bucket4j / proxy rules for production.
 */
@Component
public class RateLimitingHandshakeInterceptor implements HandshakeInterceptor {

    private static final int MAX_PER_IP = 20;

    private final Map<String, AtomicInteger> ipCounts = new ConcurrentHashMap<>();

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String ip = ((HttpServletRequest) request).getRemoteAddr();
        int current = ipCounts
                .computeIfAbsent(ip, k -> new AtomicInteger())
                .incrementAndGet();

        attributes.put("HANDSHAKE_IP", ip);        // remember for afterHandshake

        if (current > MAX_PER_IP) {
            ipCounts.get(ip).decrementAndGet();
            response.setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception ex) {

        String ip = ((HttpServletRequest) request).getRemoteAddr();
        ipCounts.getOrDefault(ip, new AtomicInteger()).decrementAndGet();
    }

}