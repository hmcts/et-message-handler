package uk.gov.hmcts.reform.ethos.ecm.consumer.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ecm.common.exceptions.InvalidMessageException;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.Msg;
import uk.gov.hmcts.ecm.common.servicebus.MessageBodyRetriever;
import uk.gov.hmcts.ecm.common.servicebus.ServiceBusSender;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm.TransferToEcmService;
import uk.gov.hmcts.reform.ethos.ecm.consumer.servicebus.MessageAutoCompletor;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class CreateUpdatesBusReceiverTaskTest {

    @Mock
    private transient ObjectMapper objectMapper;
    @Mock
    private transient MessageAutoCompletor messageCompletor;
    @Mock
    private transient ServiceBusSender serviceBusSender;
    @Mock
    private TransferToEcmService transferToEcmService;

    @InjectMocks
    private final transient CreateUpdatesBusReceiverTask createUpdatesBusReceiverTask =
        new CreateUpdatesBusReceiverTask(objectMapper, messageCompletor, serviceBusSender,
                                         transferToEcmService, 10);

    private transient Message message;
    private transient CreateUpdatesMsg msg;

    @BeforeEach
    public void setUp() {
        msg = Helper.generateCreateUpdatesMsg();
        message = createMessage(msg);
    }

    @Test
    void onMessageAsync() throws IOException {
        when(objectMapper.readValue(
            MessageBodyRetriever.getBinaryData(message.getMessageBody()),
            CreateUpdatesMsg.class
        )).thenReturn(msg);
        createUpdatesBusReceiverTask.onMessageAsync(message);
    }

    @Test
    void onMessageAsyncEmptyEthosCaseRefCollection() throws IOException {
        msg.setEthosCaseRefCollection(null);
        message = createMessage(msg);
        when(objectMapper.readValue(
            MessageBodyRetriever.getBinaryData(message.getMessageBody()),
            CreateUpdatesMsg.class
        )).thenReturn(msg);
        when(messageCompletor.completeAsync(any())).thenReturn(Helper.getCompletableFuture());
        createUpdatesBusReceiverTask.onMessageAsync(message);
        verify(objectMapper, times(2)).writeValueAsBytes(msg);
    }

    @Test
    void onMessageAsyncFailedToParse() throws IOException {
        when(objectMapper.readValue(
            MessageBodyRetriever.getBinaryData(message.getMessageBody()),
            CreateUpdatesMsg.class
        )).thenThrow(new JsonMappingException("Failed to parse"));
        createUpdatesBusReceiverTask.onMessageAsync(message);
    }

    @Test
    void onMessageAsyncException() throws IOException {
        when(objectMapper.readValue(
            MessageBodyRetriever.getBinaryData(message.getMessageBody()),
            CreateUpdatesMsg.class
        )).thenThrow(new RuntimeException("Failed"));
        createUpdatesBusReceiverTask.onMessageAsync(message);
    }

    private Message createMessage(CreateUpdatesMsg createUpdatesMsg) {
        Message busMessage = new Message();
        busMessage.setContentType("application/json");
        busMessage.setMessageId(createUpdatesMsg.getMsgId());
        busMessage.setMessageBody(getMsgBodyInBytes(createUpdatesMsg));
        busMessage.setLabel(createUpdatesMsg.getJurisdiction());
        return busMessage;
    }

    private MessageBody getMsgBodyInBytes(Msg message) {
        try {
            return MessageBody.fromBinaryData(singletonList(
                objectMapper.writeValueAsBytes(message) //default encoding is UTF-8
            ));
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }

}
