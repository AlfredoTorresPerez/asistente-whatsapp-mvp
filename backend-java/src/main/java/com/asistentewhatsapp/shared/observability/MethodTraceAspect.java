package com.asistentewhatsapp.shared.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@Aspect
@Component
@ConditionalOnProperty(prefix = "app.observability.method-tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MethodTraceAspect {

	private static final Logger LOGGER = LoggerFactory.getLogger("APP_METHOD_TRACE");

	private final TraceProperties traceProperties;
	private final TraceSanitizer traceSanitizer;

	public MethodTraceAspect(TraceProperties traceProperties, TraceSanitizer traceSanitizer) {
		this.traceProperties = traceProperties;
		this.traceSanitizer = traceSanitizer;
	}

	@Around("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Service *) || within(@org.springframework.stereotype.Repository *)")
	public Object traceMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Class<?> targetClass = joinPoint.getTarget() != null
				? AopUtils.getTargetClass(joinPoint.getTarget())
				: signature.getDeclaringType();
		String className = targetClass.getSimpleName();
		String methodName = signature.getName();
		String layer = resolveLayer(targetClass);
		String purpose = resolvePurpose(layer, className, methodName);
		String correlationId = CorrelationIdFilter.currentCorrelationId();
		boolean scheduledMethod = signature.getMethod().isAnnotationPresent(Scheduled.class);
		long start = System.nanoTime();

		if (traceProperties.isLogArguments()) {
			if (scheduledMethod) {
				LOGGER.debug(
						"[Backend - capa de servidor] Inicio metodo={} clase={} capa={} proposito=\"{}\" correlationId={} parametros={}",
						methodName, className, layer, purpose, correlationId,
						traceSanitizer.sanitizeArguments(joinPoint.getArgs(), signature.getParameterNames()));
			} else {
				LOGGER.info(
						"[Backend - capa de servidor] Inicio metodo={} clase={} capa={} proposito=\"{}\" correlationId={} parametros={}",
						methodName, className, layer, purpose, correlationId,
						traceSanitizer.sanitizeArguments(joinPoint.getArgs(), signature.getParameterNames()));
			}
		} else {
			if (scheduledMethod) {
				LOGGER.debug(
						"[Backend - capa de servidor] Inicio metodo={} clase={} capa={} proposito=\"{}\" correlationId={}",
						methodName, className, layer, purpose, correlationId);
			} else {
				LOGGER.info(
						"[Backend - capa de servidor] Inicio metodo={} clase={} capa={} proposito=\"{}\" correlationId={}",
						methodName, className, layer, purpose, correlationId);
			}
		}

		try {
			Object result = joinPoint.proceed();
			long elapsedMs = elapsedMs(start);
			if (elapsedMs >= traceProperties.getSlowExecutionThresholdMs()) {
				LOGGER.warn(
						"[Backend - capa de servidor] Metodo lento metodo={} clase={} capa={} correlationId={} tiempoMs={} umbralMs={}",
						methodName, className, layer, correlationId, elapsedMs,
						traceProperties.getSlowExecutionThresholdMs());
			}

			if (traceProperties.isLogResult()) {
				if (scheduledMethod) {
					LOGGER.debug(
							"[Backend - capa de servidor] Fin metodo={} clase={} capa={} resultado=SUCCESS correlationId={} tiempoMs={} respuesta={}",
							methodName, className, layer, correlationId, elapsedMs,
							traceSanitizer.sanitizeResult(result));
				} else {
					LOGGER.info(
							"[Backend - capa de servidor] Fin metodo={} clase={} capa={} resultado=SUCCESS correlationId={} tiempoMs={} respuesta={}",
							methodName, className, layer, correlationId, elapsedMs,
							traceSanitizer.sanitizeResult(result));
				}
			} else {
				if (scheduledMethod) {
					LOGGER.debug(
							"[Backend - capa de servidor] Fin metodo={} clase={} capa={} resultado=SUCCESS correlationId={} tiempoMs={}",
							methodName, className, layer, correlationId, elapsedMs);
				} else {
					LOGGER.info(
							"[Backend - capa de servidor] Fin metodo={} clase={} capa={} resultado=SUCCESS correlationId={} tiempoMs={}",
							methodName, className, layer, correlationId, elapsedMs);
				}
			}
			return result;
		} catch (RuntimeException exception) {
			LOGGER.error(
					"[Backend - capa de servidor] Error metodo={} clase={} capa={} resultado=ERROR_CONTROLADO correlationId={} tiempoMs={} tipoError={} mensaje={}",
					methodName, className, layer, correlationId, elapsedMs(start), exception.getClass().getSimpleName(),
					exception.getMessage(), exception);
			throw exception;
		} catch (Exception exception) {
			LOGGER.error(
					"[Backend - capa de servidor] Error metodo={} clase={} capa={} resultado=ERROR_INESPERADO correlationId={} tiempoMs={} tipoError={} mensaje={}",
					methodName, className, layer, correlationId, elapsedMs(start), exception.getClass().getSimpleName(),
					exception.getMessage(), exception);
			throw exception;
		}
	}

	private String resolveLayer(Class<?> targetClass) {
		if (targetClass.isAnnotationPresent(RestController.class)) {
			return "controlador";
		}
		if (targetClass.isAnnotationPresent(Service.class)) {
			return "servicio";
		}
		if (targetClass.isAnnotationPresent(Repository.class)) {
			return "repositorio";
		}
		return "aplicacion";
	}

	private String resolvePurpose(String layer, String className, String methodName) {
		return switch (layer) {
			case "controlador" -> "Atender solicitud HTTP y delegar la operacion al servicio correspondiente";
			case "servicio" -> "Ejecutar logica de negocio asociada a " + className + "." + methodName;
			case "repositorio" -> "Acceder a persistencia o consultar datos requeridos por la operacion";
			default -> "Ejecutar metodo de aplicacion " + className + "." + methodName;
		};
	}

	private long elapsedMs(long start) {
		return (System.nanoTime() - start) / 1_000_000L;
	}
}
