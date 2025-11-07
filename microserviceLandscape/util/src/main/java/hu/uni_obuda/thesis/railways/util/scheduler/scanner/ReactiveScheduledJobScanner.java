package hu.uni_obuda.thesis.railways.util.scheduler.scanner;

import hu.uni_obuda.thesis.railways.util.scheduler.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ReactiveScheduledJobScanner {

    private final ApplicationContext applicationContext;

    public Flux<Tuple2<String, ScheduledMethodRunnable>> scan() {
        return Flux.fromIterable(findMethods(applicationContext))
                .map(methodTuple ->
                        Tuples.of(methodTuple.getT1(), new ScheduledMethodRunnable(methodTuple.getT2(), methodTuple.getT3())));
    }

    private List<Tuple3<String, Object, Method>> findMethods(ApplicationContext applicationContext) {
        List<Tuple3<String, Object, Method>> foundMethods = new ArrayList<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            Map<Method, ScheduledJob> methods = MethodIntrospector.selectMethods(
                    targetClass,
                    (Method method) -> AnnotatedElementUtils.findMergedAnnotation(method, ScheduledJob.class)
            );

            methods.forEach((method, annotation) -> {
                String jobName = annotation.value().isEmpty()
                        ? targetClass.getSimpleName() + "#" + method.getName()
                        : annotation.value();
                foundMethods.add(Tuples.of(jobName, bean, method));
            });
        }

        return foundMethods;
    }
}

