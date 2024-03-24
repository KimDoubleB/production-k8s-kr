package com.example.demo.aop;

import dev.openfeature.sdk.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagAspect {

	private static final boolean RUN_METHOD_WHEN_EVALUATION_FAILED = false;

	private final Client openFeatureClient;

	@Around("@annotation(booleanFlag)")
	public Object booleanAspectProcessor(ProceedingJoinPoint joinPoint, BooleanFlag booleanFlag) throws Throwable {
		final String flagName = booleanFlag.value();

		if ("".equals(flagName)) {
			throw new IllegalStateException("Flag Name cannot be empty");
		}

		if (openFeatureClient.getBooleanValue(flagName, RUN_METHOD_WHEN_EVALUATION_FAILED)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("[%s] Flag is enabled.", flagName));
			}
			return joinPoint.proceed();
		}

		return null;
	}
}
