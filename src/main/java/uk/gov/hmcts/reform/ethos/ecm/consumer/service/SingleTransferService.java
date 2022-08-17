package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.client.CcdSubmitEventParams;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicValueType;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;

import java.io.IOException;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleTransferService {

    public static final String SAME_COUNTRY_EVENT_SUMMARY_TEMPLATE = "Case transferred to %s with %s";

    private final CcdClient ccdClient;
    private final SingleCreationService singleCreationService;

    public void sendTransferred(SubmitEvent submitEvent, String accessToken, UpdateCaseMsg updateCaseMsg)
        throws IOException {
        var caseTransfer = getCaseTransfer(submitEvent, updateCaseMsg);
        if (caseTransfer.isTransferSameCountry()) {
            transferSameCountry(caseTransfer, accessToken);
        } else {
            singleCreationService.sendCreation(submitEvent, accessToken, updateCaseMsg);
        }
    }

    private CaseTransfer getCaseTransfer(SubmitEvent submitEvent, UpdateCaseMsg updateCaseMsg) {
        CreationSingleDataModel creationSingleDataModel = (CreationSingleDataModel) updateCaseMsg
            .getDataModelParent();

        String caseTypeId = updateCaseMsg.getMultipleRef().equals(SINGLE_CASE_TYPE)
            ? updateCaseMsg.getCaseTypeId()
            : UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId());

        return CaseTransfer.builder()
            .caseId(submitEvent.getCaseId())
            .transferSameCountry(creationSingleDataModel.isTransferSameCountry())
            .caseTypeId(caseTypeId)
            .jurisdiction(updateCaseMsg.getJurisdiction())
            .caseData(submitEvent.getCaseData())
            .officeCT(creationSingleDataModel.getOfficeCT())
            .positionTypeCT(creationSingleDataModel.getPositionTypeCT())
            .reasonCT(creationSingleDataModel.getReasonForCT())
            .sourceEthosCaseReference(creationSingleDataModel.getSourceEthosCaseReference())
            .build();
    }

    private void transferSameCountry(CaseTransfer caseTransfer, String accessToken) throws IOException {
        var office = caseTransfer.getOfficeCT();
        var ethosCaseReference = caseTransfer.getCaseData().getEthosCaseReference();
        log.info("Creating same country transfer event for case {} and office {}", ethosCaseReference, office);
        var caseId = String.valueOf(caseTransfer.getCaseId());
        var returnedRequest = ccdClient.startCaseTransferSameCountryEccLinkedCase(accessToken,
                                                                                  caseTransfer.getCaseTypeId(),
                                                                                  caseTransfer.getJurisdiction(),
                                                                                  caseId);

        var caseData = caseTransfer.getCaseData();
        caseData.setOfficeCT(DynamicFixedListType.of(DynamicValueType.create(office, office)));
        caseData.setReasonForCT(caseTransfer.getReasonCT());
        var eventSummary = String.format(SAME_COUNTRY_EVENT_SUMMARY_TEMPLATE, office,
                                         caseTransfer.getSourceEthosCaseReference());
        var params = CcdSubmitEventParams.builder()
            .authToken(accessToken)
            .caseId(caseId)
            .caseTypeId(caseTransfer.getCaseTypeId())
            .jurisdiction(caseTransfer.getJurisdiction())
            .caseData(caseData)
            .ccdRequest(returnedRequest)
            .eventSummary(eventSummary)
            .eventDescription(caseTransfer.getReasonCT())
            .build();
        ccdClient.submitEventForCase(params);
    }
}
