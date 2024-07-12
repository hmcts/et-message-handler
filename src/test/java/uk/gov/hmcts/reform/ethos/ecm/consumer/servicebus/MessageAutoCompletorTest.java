package uk.gov.hmcts.reform.ethos.ecm.consumer.servicebus;

import com.microsoft.azure.servicebus.IQueueClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

@ExtendWith(SpringExtension.class)
class MessageAutoCompletorTest {

    @InjectMocks
    private transient MessageAutoCompletor completor;
    @Mock
    private transient IQueueClient queueClient;

    private static final UUID LOCK_TOKEN = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        completor = new MessageAutoCompletor(queueClient);
    }

    @Test
    void completeAsync() {
        completor.completeAsync(LOCK_TOKEN);
        Mockito.verify(queueClient).completeAsync(LOCK_TOKEN);
        Mockito.verifyNoMoreInteractions(queueClient);
    }

}
