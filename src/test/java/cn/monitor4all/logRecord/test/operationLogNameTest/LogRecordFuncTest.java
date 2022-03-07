package cn.monitor4all.logRecord.test.operationLogNameTest;

import cn.monitor4all.logRecord.bean.LogDTO;
import cn.monitor4all.logRecord.configuration.LogRecordAutoConfiguration;
import cn.monitor4all.logRecord.context.LogRecordContextReactive;
import cn.monitor4all.logRecord.function.LogRecordFunc;
import cn.monitor4all.logRecord.service.CustomLogListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.net.URI;
import java.util.UUID;

/**
 * @author pumbf
 * @version 1.0
 * @since 2022-01-28 21:16
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(classes = {LogRecordAutoConfiguration.class, LogRecordFuncTest.TestLogListener.class, OperationLogService.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LogRecordFuncTest {

    @Autowired
    OperationLogService operationLogService;

    @Test
    public void logRecordFuncTest() {
        System.out.println(operationLogService.test());
    }

    @Test
    public void testMonoFunction(){
        Mono<String> r = operationLogService.testReactive()
                .subscriberContext(Context.of("key", "st"));

        StepVerifier.create(r)
                .expectNext("123--")
                .verifyComplete();
    }

    @Test
    public void testFluxFunction(){
        Flux<String> r = operationLogService.testReactiveFlux()
                .subscriberContext(Context.of("key", "st"));

        StepVerifier.create(r)
                .expectNext("123--")
                .verifyComplete();
    }

    @Test
    public void testFluxFunctionVoid(){
        Flux<Void> r = operationLogService.testReactiveFluxVoid()
                .subscriberContext(Context.of("key", "st"));

        StepVerifier.create(r)
                .verifyComplete();
    }

    @TestComponent
    @Slf4j
    @Scope
    public static class TestLogListener extends CustomLogListener {

        @Override
        public void createLog(LogDTO logDTO) throws Exception {
            log.info(JSON.toJSONString(logDTO));
            Assertions.assertEquals(logDTO.getBizId(),  "test");
        }
    }


    @LogRecordFunc("test")
    public static class FunctionTest {

        @LogRecordFunc("test")
        public static String testMethod(){
            return "test";
        }
        @LogRecordFunc("test1")
        public static String testMethod1(){
            return "te";
        }
    }
}
