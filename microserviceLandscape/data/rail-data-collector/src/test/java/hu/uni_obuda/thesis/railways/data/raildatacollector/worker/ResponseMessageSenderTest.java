package hu.uni_obuda.thesis.railways.data.raildatacollector.worker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.event.HttpResponseEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseMessageSenderTest {

    @Mock
    private StreamBridge streamBridge;

    private ResponseMessageSender testedObject;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        testedObject = new ResponseMessageSenderImpl(streamBridge);

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
    void sendResponseMessage_sendFails_errorLogged() {
        HttpResponseEvent event = new HttpResponseEvent();
        when(streamBridge.send(eq("binding-out"), any(Message.class))).thenReturn(false);

        testedObject.sendResponseMessage("binding-out", event);

        verify(streamBridge, times(1)).send(eq("binding-out"), any(Message.class));
        assertTrue(listAppender.list.size() >= 2);

        ILoggingEvent infoLog = listAppender.list.get(0);
        ILoggingEvent errorLog = listAppender.list.get(1);

        assertEquals(Level.INFO, infoLog.getLevel());
        assertTrue(infoLog.getFormattedMessage().contains("Sending a response message to binding-out"));

        assertEquals(Level.ERROR, errorLog.getLevel());
        assertTrue(errorLog.getFormattedMessage().contains("Failed to send the response message to binding-out"));
    }
}
