package cn.monitor4all.logRecord.aop;

import cn.monitor4all.logRecord.annotation.OperationLog;
import cn.monitor4all.logRecord.annotation.OperationLogReactive;
import cn.monitor4all.logRecord.bean.LogDTO;
import cn.monitor4all.logRecord.context.LogRecordContext;
import cn.monitor4all.logRecord.context.LogRecordContextReactive;
import cn.monitor4all.logRecord.function.CustomFunctionRegistrar;
import cn.monitor4all.logRecord.service.CustomLogListener;
import cn.monitor4all.logRecord.service.LogService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class SystemLogAspectReactive{

    @Autowired(required = false)
    private LogService logService;

    @Autowired(required = false)
    private CustomLogListener customLogListener;

    @Autowired
    private SystemLogAspect systemLogAspect;

    private final SpelExpressionParser parser = new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Pointcut("@annotation(cn.monitor4all.logRecord.annotation.OperationLogReactive)")
    /*
    * execution(<修饰符模式>?<返回类型模式><方法名模式>(<参数模式>)<异常模式>?)
    * */
//    @Pointcut("execution(* com.yitian.survey.service.impl..*.*(..))")
    public void pointcutAnnotation(){

    }

    @Around("pointcutAnnotation()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable{
        if (!isReactive(pjp)) {
            return systemLogAspect.doAround(pjp);
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (isMono(pjp)) {
//            return ((Mono)pjp.proceed()).doOnNext(o -> {
//                doOnNext(o, pjp, stopWatch);
//            }).doOnError(throwable -> {
//                doOnError(throwable, pjp,stopWatch);
//            });
//            return ((Mono)pjp.proceed()).flatMap(o -> {
//                return Mono.subscriberContext().map(context -> {
//                    log.info("log in subscriberContext");
//                    String value = context.getOrDefault("key", "no_data");
//                    log.info(value);
//                    return value;
//                }).then(Mono.just(o));
//            }).doOnError(throwable -> {
//                doOnError(throwable, pjp,stopWatch);
//            });
            return ((Mono)pjp.proceed()).doOnNext(o -> {
                Mono.subscriberContext().map(context -> {
                    log.info("log in subscriberContext");
                    String value = context.getOrDefault("key", "no_data");
                    log.info(value);
                    return value;
                });
            }).doOnError(throwable -> {
                doOnError(throwable, pjp,stopWatch);
            });
        }else if (isFlux(pjp)) {
            return ((Flux)pjp.proceed()).doOnNext(o -> {
                doOnNext(o, pjp, stopWatch);
            }).doOnError(throwable -> {
                doOnError(throwable, pjp, stopWatch);
            });
        }


        return pjp.proceed();
    }

//    private Mono<Object> doFlatMap(Object methodReturn,ProceedingJoinPoint pjp,StopWatch stopWatch) {
//        stopWatch.stop();
//        return LogRecordContextReactive.getContext().flatMap(ctx -> {
////            log.error("get value from key:"+ctx.get("key"));
////            return Mono.just(methodReturn);
//            return resolveExpress(pjp).map(logDTO -> {
//                logDTO.setSuccess(true);
//                logDTO.setReturnStr(JSON.toJSONString(methodReturn));
//                doFinnally(logDTO, stopWatch);
//                return logDTO;
//            }).collectList().then(Mono.just(methodReturn));
//        });
////        return Mono.subscriberContext().flatMap(ctx->{
////            log.error("get value from key:"+ctx.get("key"));
//////            return Mono.just(methodReturn);
////            return resolveExpress(pjp).map(logDTO -> {
////                logDTO.setSuccess(true);
////                logDTO.setReturnStr(JSON.toJSONString(methodReturn));
////                doFinnally(logDTO, stopWatch);
////                return logDTO;
////            }).collectList().then(Mono.just(methodReturn));
////        });
//    }
    private void doOnNext(Object methodReturn,ProceedingJoinPoint pjp,StopWatch stopWatch) {
        stopWatch.stop();
        List<LogDTO> logS = resolveExpress(pjp);
        logS.forEach(logDTO -> {
            logDTO.setSuccess(true);
            logDTO.setReturnStr(JSON.toJSONString(methodReturn));
            doFinnally(logDTO, stopWatch);
        });
        Mono.subscriberContext().map(context -> {
            log.info("log in subscriberContext");
            String value = context.getOrDefault("key", "no_data");
            log.info(value);
            return value;
        }).subscribe();
    }

    private void doOnError(Object e,ProceedingJoinPoint pjp,StopWatch stopWatch) {
        if (stopWatch.isRunning())
            stopWatch.stop();
        List<LogDTO> logS = resolveExpress(pjp);
        logS.forEach(logDTO -> {
            logDTO.setSuccess(false);
            logDTO.setException(((Throwable)e).getMessage());
            doFinnally(logDTO, stopWatch);
        });
    }

    private void doFinnally(LogDTO logDTO,StopWatch stopWatch) {
        try {
            // 记录执行时间
            logDTO.setExecutionTime(stopWatch.getTotalTimeMillis());
            // 发送本地监听
            if (customLogListener != null) {
                customLogListener.createLog(logDTO);
            }
            // 发送数据管道
            if (logService != null) {
                logService.createLog(logDTO);
            }
        } catch (Throwable throwable) {
            log.error("SystemLogAspect doAround send log error", throwable);
        }
    }

    private boolean isReactive(ProceedingJoinPoint pjp) {
        Signature signature =  pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Mono".equals(returnType.getName()) || "reactor.core.publisher.Flux".equals(returnType.getName());
    }

    private boolean isFlux(ProceedingJoinPoint pjp) {
        Signature signature =  pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Flux".equals(returnType.getName());
    }

    private boolean isMono(ProceedingJoinPoint pjp) {
        Signature signature =  pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Mono".equals(returnType.getName());
    }

    public List<LogDTO> resolveExpress(JoinPoint joinPoint) {
        try {
            List<LogDTO> logDTOList = new ArrayList<>();
            Object[] arguments = joinPoint.getArgs();
            Method method = getMethod(joinPoint);
            OperationLogReactive[] annotations = method.getAnnotationsByType(OperationLogReactive.class);
            for (OperationLogReactive annotation : annotations) {
                LogDTO logDTO = new LogDTO();
                logDTOList.add(logDTO);
                String bizIdSpel = annotation.bizId();
                String msgSpel = annotation.msg();
                String bizId = bizIdSpel;
                String msg = msgSpel;
                try {
                    String[] params = discoverer.getParameterNames(method);
                    StandardEvaluationContext context = new StandardEvaluationContext();
                    CustomFunctionRegistrar.register(context);
                    if (params != null) {
                        for (int len = 0; len < params.length; len++) {
                            context.setVariable(params[len], arguments[len]);
                        }
                    }

                    // bizId 处理：直接传入字符串会抛出异常，写入默认传入的字符串
                    if (StringUtils.isNotBlank(bizIdSpel)) {
                        Expression bizIdExpression = parser.parseExpression(bizIdSpel);
                        bizId = bizIdExpression.getValue(context, String.class);
                    }

                    // msg 处理：写入默认传入的字符串
                    if (StringUtils.isNotBlank(msgSpel)) {
                        Expression msgExpression = parser.parseExpression(msgSpel);
                        Object msgObj = msgExpression.getValue(context, Object.class);
                        msg = msgObj instanceof String ? String.valueOf(msgObj) : JSON.toJSONString(msgObj, SerializerFeature.WriteMapNullValue);
                    }

                } catch (Exception e) {
                    log.error("SystemLogAspect resolveExpress error", e);
                } finally {
                    logDTO.setLogId(UUID.randomUUID().toString());
                    logDTO.setBizId(bizId);
                    logDTO.setBizType(annotation.bizType());
                    logDTO.setOperateDate(new Date());
                    logDTO.setMsg(msg);
                    logDTO.setTag(annotation.tag());
                }
            }
            return logDTOList;

        } catch (Exception e) {
            log.error("SystemLogAspect resolveExpress error", e);
            return new ArrayList<>();
        }
    }

    protected Method getMethod(JoinPoint joinPoint) {
        Method method = null;
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature ms = (MethodSignature) signature;
            Object target = joinPoint.getTarget();
            method = target.getClass().getMethod(ms.getName(), ms.getParameterTypes());
        } catch (NoSuchMethodException e) {
            log.error("SystemLogAspect getMethod error", e);
        }
        return method;
    }
}
