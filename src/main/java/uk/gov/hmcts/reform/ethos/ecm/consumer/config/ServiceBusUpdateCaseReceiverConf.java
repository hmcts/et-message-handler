package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ethos.ecm.consumer.tasks.UpdateCaseBusReceiverTask;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@AutoConfigureAfter(ServiceBusCreateUpdatesReceiverConf.class)
@Configuration
@SuppressWarnings("PMD.DoNotUseThreads")
public class ServiceBusUpdateCaseReceiverConf {

    private final IQueueClient updateCaseListenClient;

    private final UpdateCaseBusReceiverTask updateCaseBusReceiverTask;

    private static final ExecutorService UPDATE_CASE_LISTEN_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "update-case-queue-listen"));

    private static final MessageHandlerOptions MESSAGE_HANDLER_OPTIONS =
        new MessageHandlerOptions(1, false, Duration.ofMinutes(5));

    @PostConstruct()
    public void registerMessageHandlers() throws InterruptedException, ServiceBusException {
        updateCaseListenClient.registerMessageHandler(
            updateCaseBusReceiverTask,
            MESSAGE_HANDLER_OPTIONS,
            UPDATE_CASE_LISTEN_EXECUTOR
        );
    }

    public ServiceBusUpdateCaseReceiverConf(
        @Qualifier("update-case-listen-client") IQueueClient updateCaseListenClient,
        UpdateCaseBusReceiverTask updateCaseBusReceiverTask) {
        this.updateCaseListenClient = updateCaseListenClient;
        this.updateCaseBusReceiverTask = updateCaseBusReceiverTask;
    }

}
