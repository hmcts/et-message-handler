package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${multithreading.create-updates-bus-receiver.maxConcurrentCalls}")
    private int maxConcurrentCalls;

    private final IQueueClient createUpdatesListenClient;

    private final CreateUpdatesBusReceiverTask createUpdatesBusReceiverTask;

    private static final ExecutorService CREATE_UPDATES_LISTEN_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "create-updates-queue-listen")
        );

    @PostConstruct()
    public void registerMessageHandlers() throws InterruptedException, ServiceBusException {
        MessageHandlerOptions messageHandlerOptions =
            new MessageHandlerOptions(maxConcurrentCalls,
                                      false,
                                      Duration.ofMinutes(5));

        createUpdatesListenClient.registerMessageHandler(
            createUpdatesBusReceiverTask,
            messageHandlerOptions,
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
