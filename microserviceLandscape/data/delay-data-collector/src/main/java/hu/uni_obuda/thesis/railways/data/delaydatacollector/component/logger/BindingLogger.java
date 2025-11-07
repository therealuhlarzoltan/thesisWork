package hu.uni_obuda.thesis.railways.data.delaydatacollector.component.logger;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BindingLogger {

    @Autowired
    private org.springframework.cloud.stream.binding.BindingService bindingService;

    @PostConstruct
    public void showBindings() {
        for (var bName : bindingService.getConsumerBindingNames()) {
            log.info("Consumer binding {}", bName);
        }
        for (var bName : bindingService.getProducerBindingNames()) {
            log.info("Producer binding {}", bName);
        }
    }
}
