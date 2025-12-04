package hu.uni_obuda.thesis.railways.data.delaydatacollector.workers.messaging.senders;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import hu.uni_obuda.thesis.railways.data.delaydatacollector.worker.messaging.sender.MessageSenderImpl;
import hu.uni_obuda.thesis.railways.data.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageSenderTest {

    @Mock
    private StreamBridge streamBridge;
    @Mock
    private Event<String, Object> event;

    private MessageSenderImpl testedObject;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        testedObject = new MessageSenderImpl(streamBridge);

        logger = (Logger) LoggerFactory.getLogger(MessageSenderImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(event.getKey()).thenReturn("partition-key-123");
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    private List<String> loggedMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    void sendMessage_sendSuccess_buildsMessageAndLogsInfo() {
        String bindingName = "testBinding";

        when(streamBridge.send(eq(bindingName), any(Message.class))).thenReturn(true);

        testedObject.sendMessage(bindingName, event);

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq(bindingName), messageCaptor.capture());

        Message<?> sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getPayload()).isEqualTo(event);
        assertThat(sentMessage.getHeaders().get("partitionKey")).isEqualTo("partition-key-123");

        List<String> messages = loggedMessages();
        assertThat(messages).anyMatch(msg -> msg.contains("Sending a message to " + bindingName));
        assertThat(messages).anyMatch(msg -> msg.contains("Successfully sent a message to " + bindingName));
    }
}
