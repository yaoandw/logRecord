package cn.monitor4all.logRecord.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
public class LogRecordContextReactive {

    private String msg;

    private static final Map<String, StandardEvaluationContext> requestIdContextMap = new HashMap<>();

    public static StandardEvaluationContext getContext(String requestId) {
        StandardEvaluationContext context = requestIdContextMap.get(requestId);
        if (context == null) {
            context = new StandardEvaluationContext();
            requestIdContextMap.put(requestId, context);
        }
        return context;
    }

    public static void putVariables(String requestId, String key, Object value) {
        getContext(requestId).setVariable(key, value);
    }

    public static void clearContext(String requestId) {
        requestIdContextMap.remove(requestId);
    }

    public static final Class<ServerHttpRequest> CONTEXT_KEY = ServerHttpRequest.class;

    public static Mono<ServerHttpRequest> getRequest() {
        return Mono.subscriberContext()
                .map(ctx -> ctx.get(CONTEXT_KEY));
    }
}
