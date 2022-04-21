package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;

@Slf4j
@RequiredArgsConstructor
@Service
public class SingleReadingService {

    private final CcdClient ccdClient;
    private final UserService userService;
    private final SingleUpdateService singleUpdateService;
    private final SingleTransferService singleTransferService;

    public void sendUpdateToSingleLogic(UpdateCaseMsg updateCaseMsg) throws IOException {
        String accessToken = userService.getAccessToken();
        List<SubmitEvent> submitEvents = retrieveSingleCase(accessToken, updateCaseMsg);

        if (submitEvents != null && !submitEvents.isEmpty()) {
            if (updateCaseMsg.getDataModelParent() instanceof CreationSingleDataModel) {
                singleTransferService.sendTransferred(submitEvents.get(0), accessToken, updateCaseMsg);
            } else {
                singleUpdateService.sendUpdate(submitEvents.get(0), accessToken, updateCaseMsg);
            }
        } else {
            log.warn("No submit events found for msg id {} with case reference {}", updateCaseMsg.getMsgId(),
                     updateCaseMsg.getEthosCaseReference());
        }
    }

    private List<SubmitEvent> retrieveSingleCase(String accessToken, UpdateCaseMsg updateCaseMsg) throws IOException {
        Objects.requireNonNull(updateCaseMsg.getEthosCaseReference(), "No ethosCaseReference found");
        String caseType = !updateCaseMsg.getMultipleRef().equals(SINGLE_CASE_TYPE)
            ? UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())
            : updateCaseMsg.getCaseTypeId();

        return ccdClient.retrieveCasesElasticSearch(
            accessToken,
            caseType,
            new ArrayList<>(Collections.singletonList(updateCaseMsg.getEthosCaseReference())));
    }
}
