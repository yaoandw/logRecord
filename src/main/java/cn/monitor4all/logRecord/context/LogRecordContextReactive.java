package cn.monitor4all.logRecord.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
public class LogRecordContextReactive {

    private String msg;
    private String userId;
    private String username;
    private String nickname;

    public static Mono<StandardEvaluationContext> getSpelContext() {
        return Mono.subscriberContext().map(context -> {
            StandardEvaluationContext spelContext = new StandardEvaluationContext();
            context.stream().forEach(objectObjectEntry -> {
                spelContext.setVariable(objectObjectEntry.getKey().toString(),objectObjectEntry.getValue());
            });
            return spelContext;
        });
    }

}
