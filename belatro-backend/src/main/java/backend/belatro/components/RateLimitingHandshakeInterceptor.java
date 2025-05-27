package backend.belatro.components;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingHandshakeInterceptor implements HandshakeInterceptor {

    private static final int MAX_PER_IP = 20;
    private final Map<String, AtomicInteger> ipCounts = new ConcurrentHashMap<>();

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String ip = extractIp(request);
        int current = ipCounts.computeIfAbsent(ip, k -> new AtomicInteger())
                              .incrementAndGet();

        attributes.put("HANDSHAKE_IP", ip);

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

        String ip = extractIp(request);
        ipCounts.getOrDefault(ip, new AtomicInteger()).decrementAndGet();
    }

    /**
     * Safely obtain the Client IP from the ServerHttpRequest.
     */
    private static String extractIp(ServerHttpRequest request) {

        // 1️⃣ First try the low-level remote address
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }

        // 2️⃣ Fallback: unwrap the servlet request if present
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getRemoteAddr();
        }

        return "unknown";
    }
}