package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ethos.ecm.consumer.tasks.CreateUpdatesBusReceiverTask;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@SuppressWarnings("PMD.DoNotUseThreads")
@AutoConfigureAfter(ServiceBusSenderConfiguration.class)
@Configuration
public class ServiceBusCreateUpdatesReceiverConf {

    private final IQueueClient createUpdatesListenClient;

    private final CreateUpdatesBusReceiverTask createUpdatesBusReceiverTask;
    private static final MessageHandlerOptions MESSAGE_HANDLER_OPTIONS =
        new MessageHandlerOptions(10, false, Duration.ofMinutes(5));

    private static final ExecutorService CREATE_UPDATES_LISTEN_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "create-updates-queue-listen")
        );

    @PostConstruct()
    public void registerMessageHandlers() throws InterruptedException, ServiceBusException {
        createUpdatesListenClient.registerMessageHandler(
            createUpdatesBusReceiverTask,
            MESSAGE_HANDLER_OPTIONS,
            CREATE_UPDATES_LISTEN_EXECUTOR
        );
    }

    public ServiceBusCreateUpdatesReceiverConf(
        @Qualifier("create-updates-listen-client") IQueueClient createUpdatesListenClient,
        CreateUpdatesBusReceiverTask createUpdatesBusReceiverTask) {
        this.createUpdatesListenClient = createUpdatesListenClient;
        this.createUpdatesBusReceiverTask = createUpdatesBusReceiverTask;
    }

}
