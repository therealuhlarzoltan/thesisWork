package hu.uni_obuda.thesis.railways.data.weatherdatacollector.workers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ResponseMessageSenderTest {

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private ResponseMessageSenderImpl testedObject;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(ResponseMessageSenderImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(ResponseMessageSenderImpl.class);
        logger.detachAppender(listAppender);
    }

    @Test
    void sendResponseMessage_whenSendFails_logsInfoAndError() {
        String bindingName = "testBinding";
        HttpResponseEvent event = mock(HttpResponseEvent.class);

        when(streamBridge.send(eq(bindingName), any(Message.class))).thenReturn(false);

        testedObject.sendResponseMessage(bindingName, event);

        verify(streamBridge, times(1)).send(eq(bindingName), any(Message.class));

        assertThat(listAppender.list).hasSize(2);

        ILoggingEvent infoLog = listAppender.list.getFirst();
        assertThat(infoLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(infoLog.getFormattedMessage())
                .isEqualTo("Sending a response message to " + bindingName);

        ILoggingEvent errorLog = listAppender.list.get(1);
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage())
                .isEqualTo("Failed to send the response message to " + bindingName);
    }
}
