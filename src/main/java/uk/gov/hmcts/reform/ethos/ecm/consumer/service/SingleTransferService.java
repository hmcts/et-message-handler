package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.client.CcdSubmitEventParams;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.ecm.common.model.bulk.types.DynamicValueType;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;

import java.io.IOException;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;

@Slf4j
@Service
public class SingleTransferService {

    public static final String SAME_COUNTRY_EVENT_SUMMARY_TEMPLATE = "Case transferred to %s with %s";

    private final CcdClient ccdClient;
    private final SingleCreationService singleCreationService;

    public SingleTransferService(CcdClient ccdClient, SingleCreationService singleCreationService) {
        this.ccdClient = ccdClient;
        this.singleCreationService = singleCreationService;
    }

    public void sendTransferred(SubmitEvent submitEvent, String accessToken, UpdateCaseMsg updateCaseMsg)
        throws IOException {
        var caseTransfer = getCaseTransfer(submitEvent, updateCaseMsg);
        if (caseTransfer.isTransferSameCountry()) {
            transferSameCountry(caseTransfer, accessToken);
        } else {
            transferDifferentCountry(caseTransfer, accessToken);
            singleCreationService.sendCreation(submitEvent, accessToken, updateCaseMsg);
        }
    }

    private CaseTransfer getCaseTransfer(SubmitEvent submitEvent, UpdateCaseMsg updateCaseMsg) {
        var creationSingleDataModel = ((CreationSingleDataModel) updateCaseMsg.getDataModelParent());

        String caseTypeId = !updateCaseMsg.getMultipleRef().equals(SINGLE_CASE_TYPE)
            ? UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())
            : updateCaseMsg.getCaseTypeId();

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
        log.info("Creating same country transfer event for case {} and office {}",
                 caseTransfer.getCaseData().getEthosCaseReference(), office);
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

    private void transferDifferentCountry(CaseTransfer caseTransfer, String accessToken) throws IOException {
        var caseData = caseTransfer.getCaseData();
        caseData.setLinkedCaseCT("Transferred to " + caseTransfer.getOfficeCT());

        log.info("Setting positionType to {} for case {} ", caseTransfer.getPositionTypeCT(),
                 caseData.getEthosCaseReference());
        caseData.setPositionType(caseTransfer.getPositionTypeCT());
        caseData.setPositionTypeCT(caseTransfer.getPositionTypeCT());
        caseData.setReasonForCT(caseTransfer.getReasonCT());

        var caseId = String.valueOf(caseTransfer.getCaseId());
        var returnedRequest = ccdClient.startCaseTransfer(accessToken, caseTransfer.getCaseTypeId(),
                                                          caseTransfer.getJurisdiction(), caseId);
        ccdClient.submitEventForCase(accessToken, caseData, caseTransfer.getCaseTypeId(),
                                     caseTransfer.getJurisdiction(), returnedRequest, caseId);
    }
}
