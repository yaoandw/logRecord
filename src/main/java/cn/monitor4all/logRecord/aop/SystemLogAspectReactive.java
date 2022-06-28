package cn.monitor4all.logRecord.aop;

import cn.monitor4all.logRecord.annotation.OperationLog;
import cn.monitor4all.logRecord.annotation.OperationLogReactive;
import cn.monitor4all.logRecord.bean.LogDTO;
import cn.monitor4all.logRecord.configuration.LogReactiveRequestContextHolder;
import cn.monitor4all.logRecord.context.LogRecordContext;
import cn.monitor4all.logRecord.context.LogRecordContextReactive;
import cn.monitor4all.logRecord.function.CustomFunctionRegistrar;
import cn.monitor4all.logRecord.service.CustomLogListener;
import cn.monitor4all.logRecord.service.LogService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.util.*;

@Aspect
@Component
@Slf4j
public class SystemLogAspectReactive {

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
    public void pointcutAnnotation() {

    }

    @Around("pointcutAnnotation()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
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
            return ((Mono) pjp.proceed()).flatMap(o -> {
                return doFlatMap(o, pjp, stopWatch);
            }).switchIfEmpty(Mono.defer(() -> doFlatMap(null, pjp, stopWatch)))
                    .doOnError(throwable -> {
                        doOnError(throwable, pjp, stopWatch);
                    });
        } else if (isFlux(pjp)) {
//            return ((Flux)pjp.proceed()).doOnNext(o -> {
//                doOnNext(o, pjp, stopWatch);
//            }).doOnError(throwable -> {
//                doOnError(throwable, pjp, stopWatch);
//            });
            return ((Flux) pjp.proceed()).flatMap(o -> {
                return doFlatMap(o, pjp, stopWatch);
            }).switchIfEmpty(Mono.defer(() -> doFlatMap(null, pjp, stopWatch)))
                    .doOnError(throwable -> {
                        doOnError(throwable, pjp, stopWatch);
                    });
//            return ((Flux)pjp.proceed()).thenMany(doFlatMap(null, pjp, stopWatch))
//                    .doOnError(throwable -> {
//                doOnError(throwable, pjp, stopWatch);
//            });
        }


        return pjp.proceed();
    }

    private Mono<Object> doFlatMap(Object methodReturn, ProceedingJoinPoint pjp, StopWatch stopWatch) {
        stopWatch.stop();
        return resolveExpressReactive(pjp).map(logDTO -> {
            logDTO.setSuccess(true);
            logDTO.setReturnStr(JSON.toJSONString(methodReturn));
            doFinnally(logDTO, stopWatch);
            return logDTO;
        }).then(methodReturn != null ? Mono.just(methodReturn) : Mono.empty());
    }

    private void doOnNext(Object methodReturn, ProceedingJoinPoint pjp, StopWatch stopWatch) {
        stopWatch.stop();
//        List<LogDTO> logS = resolveExpress(pjp);
//        logS.forEach(logDTO -> {
//            logDTO.setSuccess(true);
//            logDTO.setReturnStr(JSON.toJSONString(methodReturn));
//            doFinnally(logDTO, stopWatch);
//        });
        resolveExpressReactive(pjp).map(logDTO -> {
            logDTO.setSuccess(true);
            logDTO.setReturnStr(JSON.toJSONString(methodReturn));
            doFinnally(logDTO, stopWatch);
            return logDTO;
        }).subscribe();
    }

    private void doOnError(Object e, ProceedingJoinPoint pjp, StopWatch stopWatch) {
        if (stopWatch.isRunning())
            stopWatch.stop();
//        List<LogDTO> logS = resolveExpress(pjp);
//        logS.forEach(logDTO -> {
//            logDTO.setSuccess(false);
//            logDTO.setException(((Throwable)e).getMessage());
//            doFinnally(logDTO, stopWatch);
//        });
        resolveExpressReactive(pjp).map(logDTO -> {
            logDTO.setSuccess(false);
            logDTO.setException(((Throwable) e).getMessage());
            doFinnally(logDTO, stopWatch);
            return logDTO;
        }).subscribe();
    }

    private void doFinnally(LogDTO logDTO, StopWatch stopWatch) {
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
        Signature signature = pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Mono".equals(returnType.getName()) || "reactor.core.publisher.Flux".equals(returnType.getName());
    }

    private boolean isFlux(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Flux".equals(returnType.getName());
    }

    private boolean isMono(ProceedingJoinPoint pjp) {
        Signature signature = pjp.getSignature();
        Class returnType = ((MethodSignature) signature).getReturnType();
        return "reactor.core.publisher.Mono".equals(returnType.getName());
    }

    public Flux<LogDTO> resolveExpressReactive(JoinPoint joinPoint) {
        return LogRecordContextReactive.getSpelContext().flatMapMany(context -> {
            return LogReactiveRequestContextHolder.getRequest().flatMapMany(request -> {
                return resolveExpress(joinPoint, context, request);
            }).switchIfEmpty(resolveExpress(joinPoint, context, null))
                    .onErrorResume(e -> {
                        log.error("LogReactiveRequestContextHolder.getRequest() error:" + e.getMessage());
                        if (e instanceof NoSuchElementException) { //java.util.NoSuchElementException: Context is empty
                            return resolveExpress(joinPoint, context, null);
                        }
                        return Flux.error(e);
                    });

        }).onErrorResume(e -> {
            log.error("SystemLogAspect resolveExpressReactive error", e);
            return Flux.empty();
        });
    }

    private Flux<LogDTO> resolveExpress(JoinPoint joinPoint, StandardEvaluationContext context, ServerHttpRequest request) {
        List<LogDTO> logDTOList = new ArrayList<>();
        Object[] arguments = joinPoint.getArgs();
        Method method = getMethod(joinPoint);
        OperationLogReactive[] annotations = method.getAnnotationsByType(OperationLogReactive.class);
        return Flux.fromArray(annotations).flatMap(annotation -> {

            LogDTO logDTO = new LogDTO();
            logDTOList.add(logDTO);
            String bizIdSpel = annotation.bizId();
            String msgSpel = annotation.msg();
            String tagSpel = annotation.tag();
            String bizTypeSpel = annotation.bizType();
            String bizId = bizIdSpel;
            String msg = msgSpel;
            String tag = tagSpel;
            String bizType = bizTypeSpel;
            try {
                String[] params = discoverer.getParameterNames(method);
                CustomFunctionRegistrar.register(context);
                if (params != null) {
                    for (int len = 0; len < params.length; len++) {
                        context.setVariable(params[len], arguments[len]);
                    }
                }

                // bizId 处理：直接传入字符串会抛出异常，写入默认传入的字符串
                bizId = systemLogAspect.parseSpel(bizIdSpel, context);

                // msg 处理：写入默认传入的字符串
                msg = systemLogAspect.parseSpel(msgSpel, context);

                tag = systemLogAspect.parseSpel(tagSpel, context);
                bizType = systemLogAspect.parseSpel(bizTypeSpel, context);

            } catch (Exception e) {
                log.error("SystemLogAspect resolveExpress error", e);
            } finally {
                logDTO.setLogId(UUID.randomUUID().toString());
                logDTO.setBizId(bizId);
                logDTO.setBizType(bizType);
                logDTO.setOperateDate(new Date());
                logDTO.setMsg(msg);
                logDTO.setTag(tag);
                logDTO.setIp(getIp(request));
                logDTO.setDevice(getSystemInfoMd5(request));
            }
            return Mono.just(logDTO);
        });
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

    private String getIp(ServerHttpRequest request) {
        if (request != null) {
            String ip = request.getHeaders().getFirst("x-forwarded-for");
            return org.springframework.util.StringUtils.hasLength(ip) ? ip : "unknown";
        }
        return "unknown";
    }

    private String getSystemInfoMd5(ServerHttpRequest request) {
        if (request != null) {
            String md5 = request.getHeaders().getFirst("sys");
            return StringUtils.hasLength(md5) ? md5 : "unknown";
        }
        return "unknown";
    }
}
