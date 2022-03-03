package cn.monitor4all.logRecord.test.operationLogNameTest;

import cn.monitor4all.logRecord.annotation.OperationLog;
import cn.monitor4all.logRecord.annotation.OperationLogReactive;
import cn.monitor4all.logRecord.context.LogRecordContextReactive;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

    @OperationLogReactive(bizId = "#test_test() + '--' + #key", bizType = "1234")
    public Mono<String> testReactive() {
        return Mono.just("123--");
    }
}
