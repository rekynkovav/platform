package com.platform.aspect;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class TimedAspect {

    private final MeterRegistry meterRegistry;

    public TimedAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(timed)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        String[] tags = timed.extraTags();
        String timerName = timed.value().isEmpty() ?
            className + "." + methodName : timed.value();

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder(timerName)
                .description("Execution time of " + className + "." + methodName)
                .tags(tags)
                .register(meterRegistry));
            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder(timerName)
                .description("Execution time of " + className + "." + methodName)
                .tags("error", e.getClass().getSimpleName())
                .register(meterRegistry));
            throw e;
        }
    }

    @Around("@annotation(io.micrometer.core.annotation.Counted)")
    public Object countMethodCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        String counterName = "method.calls." + className + "." + methodName;
        meterRegistry.counter(counterName).increment();

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            meterRegistry.counter(counterName, "error", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }
}