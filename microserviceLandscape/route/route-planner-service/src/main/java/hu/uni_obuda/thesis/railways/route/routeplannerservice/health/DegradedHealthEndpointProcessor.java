package hu.uni_obuda.thesis.railways.route.routeplannerservice.health;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

@Slf4j
public non-sealed class DegradedHealthEndpointProcessor extends DegradedHealthStatusProcessor<HealthEndpoint> {

    public DegradedHealthEndpointProcessor() {
        super(HealthEndpoint.class);
    }

    @Override
    protected Object performPostProcessing(@NonNull Object bean, @NonNull String beanName) {
        log.info("Creating proxy factory for bean with class {} and name {}", bean.getClass(), beanName);
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(new HealthEndpointAggregateDescriptorsAdvisor());
        log.info("Added advisor for method health() for bean with class {} and name {}", bean.getClass(), beanName);
        return proxyFactory.getProxy();
    }

    private static final class HealthEndpointAggregateDescriptorsAdvisor extends StaticMethodMatcherPointcutAdvisor {

        public HealthEndpointAggregateDescriptorsAdvisor() {
           super((MethodInterceptor) (MethodInvocation invocation) -> {
                Object result = invocation.proceed();
                return DegradedHealthEndpointProcessor.originalHealthDescriptorOrNewSystemHealth(result);
           });
        }

        @Override
        public boolean matches(@NonNull Method method, @NonNull Class<?> targetClass) {
            if (!method.getName().equals("health")) {
                return false;
            }
            if (method.getParameterTypes().length != 0) {
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
                    return HealthEndpointAggregateDescriptorsAdvisor.this.matches(method, targetClass);
                }
            };
        }
    }
}