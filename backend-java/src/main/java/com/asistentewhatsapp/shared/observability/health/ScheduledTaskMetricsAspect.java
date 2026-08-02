package com.asistentewhatsapp.shared.observability.health;

import com.asistentewhatsapp.shared.observability.BusinessMetrics;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(prefix = "app.observability.method-tracing", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ScheduledTaskMetricsAspect {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskMetricsAspect.class);

	private final BusinessMetrics businessMetrics;
	private final ScheduledTaskRunRegistry registry;
	private final ObjectProvider<Tracer> tracerProvider;

	public ScheduledTaskMetricsAspect(BusinessMetrics businessMetrics, ScheduledTaskRunRegistry registry,
			ObjectProvider<Tracer> tracerProvider) {
		this.businessMetrics = businessMetrics;
		this.registry = registry;
		this.tracerProvider = tracerProvider;
	}

	@Around("execution(@org.springframework.scheduling.annotation.Scheduled * com.asistentewhatsapp..*(..))")
	public Object traceScheduledTask(ProceedingJoinPoint joinPoint) throws Throwable {
		String task = resolveTaskName(joinPoint);
		Span span = null;
		Tracer.SpanInScope scope = null;
		Tracer tracer = tracerProvider.getIfAvailable();
		if (tracer != null) {
			span = tracer.nextSpan().name("tarea-programada." + task).start();
			scope = tracer.withSpan(span);
		}
		try {
			Object result = joinPoint.proceed();
			businessMetrics.recordTareaProgramadaExitosa(task);
			registry.markSuccess(task);
			return result;
		} catch (Throwable throwable) {
			businessMetrics.recordTareaProgramadaFallida(task);
			registry.markFailure(task);
			if (span != null) {
				span.error(throwable);
			}
			LOGGER.error("[Backend - tarea programada] Fallo tarea={} tipoError={}", task,
					throwable.getClass().getSimpleName(), throwable);
			throw throwable;
		} finally {
			if (scope != null) {
				scope.close();
			}
			if (span != null) {
				span.end();
			}
		}
	}

	private String resolveTaskName(ProceedingJoinPoint joinPoint) {
		String className = joinPoint.getTarget().getClass().getSimpleName();
		String methodName = joinPoint.getSignature().getName();
		String raw = className + "." + methodName;
		return raw.toLowerCase().replaceAll("[^a-z0-9_.]+", "_").replaceAll("_+", "_");
	}
}
