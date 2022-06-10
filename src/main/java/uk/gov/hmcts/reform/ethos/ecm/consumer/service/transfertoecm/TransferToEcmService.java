package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.TransferToEcmDataModel;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.UserService;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferToEcmService {
    private final CcdClient ccdClient;
    private final UserService userService;
    private final CreateEcmSingleService createEcmSingleService;

    public void transferToEcm(CreateUpdatesMsg createUpdatesMsg) throws IOException {
        var accessToken = userService.getAccessToken();
        if (!(createUpdatesMsg.getDataModelParent() instanceof TransferToEcmDataModel)) {
            log.warn("Invalid model state for messageID {}", createUpdatesMsg.getMsgId());
            return;
        } else {
            log.info("Searching for cases {} to transfer to ECM", createUpdatesMsg.getEthosCaseRefCollection());
        }

        var submitEvents = ccdClient.retrieveCasesElasticSearch(
            accessToken, createUpdatesMsg.getCaseTypeId(), createUpdatesMsg.getEthosCaseRefCollection());

        if (!submitEvents.isEmpty()) {
            log.info("Transferring cases {} to ECM", createUpdatesMsg.getEthosCaseRefCollection());
            for (var submitEvent : submitEvents) {
                createEcmSingleService.sendCreation(submitEvent, accessToken, createUpdatesMsg);
            }
        } else {
            log.warn("No cases found for messageID {} and case references {}", createUpdatesMsg.getMsgId(),
                     createUpdatesMsg.getEthosCaseRefCollection());
        }

    }
}
