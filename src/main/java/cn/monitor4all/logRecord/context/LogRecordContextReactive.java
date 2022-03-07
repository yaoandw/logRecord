package cn.monitor4all.logRecord.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;

@Slf4j
@Data
public class LogRecordContextReactive {

    private String msg;
    private String userId;
    private String tag;
    private String bizId;

    public static Mono<StandardEvaluationContext> getSpelContext() {
        return Mono.subscriberContext().map(context -> {
            StandardEvaluationContext spelContext = new StandardEvaluationContext();
            context.stream().forEach(objectObjectEntry -> {
                spelContext.setVariable(objectObjectEntry.getKey().toString(),objectObjectEntry.getValue());
            });
            Map<String, String> mdcMap = MDC.getCopyOfContextMap();
            if (mdcMap != null){
                mdcMap.keySet().forEach(key->{
                    String value = mdcMap.get(key);
                    spelContext.setVariable(key, value);
                });
            }

            try {

                Class spelContextClass = spelContext.getClass();
                Field variables = spelContextClass.getDeclaredField("variables");
                variables.setAccessible(true);
                Map<String, Object> spelVariables = (Map<String, Object>) variables.get(spelContext);
                spelVariables.keySet().forEach(key->{
                    log.info("key:" + key + ", value:" + spelVariables.get(key));
                });
            }catch (NoSuchFieldException e) {

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return spelContext;
        });
    }

}
