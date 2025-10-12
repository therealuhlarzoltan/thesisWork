package hu.uni_obuda.thesis.railways.route.routeplannerservice.health;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.lang.NonNull;

import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@Slf4j
public non-sealed class DegradedReactiveHealthEndpointExtensionProcessor extends DegradedHealthStatusProcessor<ReactiveHealthEndpointWebExtension> {

    public DegradedReactiveHealthEndpointExtensionProcessor() {
        super(ReactiveHealthEndpointWebExtension.class);
    }

    @Override
    protected Object performPostProcessing(@NonNull Object bean, @NonNull String beanName) {
        log.info("Creating proxy factory for bean with class {} and name {}", bean.getClass(), beanName);
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(new ReactiveHealthEndpointWebExtensionAggregateDescriptorsAdvisor());
        log.info("Added advisor for method aggregateDescriptors(ApiVersion, Map, StatusAggregator, boolean, Set) for bean with class {} and name {}", bean.getClass(), beanName);
        return proxyFactory.getProxy();
    }

    private static final class ReactiveHealthEndpointWebExtensionAggregateDescriptorsAdvisor extends StaticMethodMatcherPointcutAdvisor {

        public ReactiveHealthEndpointWebExtensionAggregateDescriptorsAdvisor() {
            super((MethodInterceptor) (MethodInvocation invocation) -> {
                Mono<?> result = (Mono<?>) invocation.proceed();
                return DegradedHealthEndpointProcessor.originalHealthDescriptorOrNewSystemHealth(result);
            });
        }

        @Override
        public boolean matches(@NonNull Method method, @NonNull Class<?> targetClass) {
            if (!method.getName().equals("health")) {
                return false;
            }
            if (method.getParameterTypes().length != 3) {
                return false;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean signatureMatches = parameterTypes[0] == ApiVersion.class && parameterTypes[1] == WebServerNamespace.class
                    && parameterTypes[2] == SecurityContext.class;
            if (!signatureMatches) {
                return false;
            }
            log.info("Method to decorate was indeed found inside bean with class {}", HealthEndpoint.class);
            return true;
        }

        @NonNull
        @Override
        public Pointcut getPointcut() {
            return new StaticMethodMatcherPointcut() {
                @Override
                public boolean matches(@NonNull Method method, @NonNull Class<?> targetClass) {
                    return ReactiveHealthEndpointWebExtensionAggregateDescriptorsAdvisor.this.matches(method, targetClass);
                }
            };
        }
    }
}