package cn.monitor4all.logRecord.test.operationLogNameTest;

import cn.monitor4all.logRecord.annotation.OperationLog;
import cn.monitor4all.logRecord.annotation.OperationLogReactive;
import cn.monitor4all.logRecord.context.LogRecordContextReactive;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * @author pumbf
 * @version 1.0
 * @since 2022-01-28 21:43
 */
@TestComponent
public class OperationLogService {

    @OperationLog(bizId = "#test_test()", bizType = "1234")
    public String test() {
        return "123";
    }

    @OperationLogReactive(bizId = "#test_test1() + #key", bizType = "1234")
    public Mono<String> testReactive() {
        return Mono.just("123--");
    }
    @OperationLogReactive(bizId = "#test_test1() + #key2", bizType = "1234")
    public Flux<String> testReactiveFlux() {
        return Flux.just("123--").flatMap(s -> {
            return Mono.just(s).subscriberContext(ctx->ctx.put("key2","st"));
        }).subscriberContext(Context.of("key1","value1"));
//        return Flux.error(new Exception("cuowu"));
    }

    @OperationLogReactive(bizId = "#test_test1() + #key2", bizType = "1234")
    public Flux<Void> testReactiveFluxVoid() {
        return Flux.just("123--").flatMap(s -> {
            return Mono.just(s).subscriberContext(ctx->ctx.put("key2","st"));
        }).subscriberContext(Context.of("key1","value1")).thenMany(Flux.empty());
//        return Flux.error(new Exception("cuowu"));
    }
}
