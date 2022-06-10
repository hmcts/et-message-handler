package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.TransferToEcmDataModel;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm.TransferToEcmCaseDataHelper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Service
public class CreateEcmSingleService {

    private final CcdClient ccdClient;

    public void sendCreation(SubmitEvent oldSubmitEvent, String accessToken, CreateUpdatesMsg createUpdatesMsg)
        throws IOException {
        var transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        var caseTypeId = TribunalOffice.isScotlandOffice(transferToEcmDataModel.getOfficeCT())
            ? TribunalOffice.SCOTLAND.getOfficeName() : transferToEcmDataModel.getOfficeCT();
        var positionTypeCT = transferToEcmDataModel.getPositionTypeCT();
        var reasonForCT = transferToEcmDataModel.getReasonForCT();
        var ccdGatewayBaseUrl = transferToEcmDataModel.getCcdGatewayBaseUrl();
        var jurisdiction = createUpdatesMsg.getJurisdiction();
        var caseId = String.valueOf(oldSubmitEvent.getCaseId());

        transferNewCase(oldSubmitEvent, caseId, caseTypeId, ccdGatewayBaseUrl, positionTypeCT,
                        jurisdiction, accessToken, reasonForCT);
    }

    private void transferNewCase(SubmitEvent oldSubmitEvent, String caseId, String caseTypeId, String ccdGatewayBaseUrl,
                                 String positionTypeCT, String jurisdiction, String accessToken, String reasonForCT)
        throws IOException {
        var newCaseDetailsCt = createCaseDetailsCaseTransfer(oldSubmitEvent.getCaseData(), caseId, caseTypeId,
                                                             ccdGatewayBaseUrl, positionTypeCT, jurisdiction,
                                                             oldSubmitEvent.getState(), reasonForCT);

        var etCCD = (uk.gov.hmcts.et.common.model.ccd.CaseDetails) TransferToEcmCaseDataHelper.objectMapper(
            newCaseDetailsCt, uk.gov.hmcts.et.common.model.ccd.CaseDetails.class
        );

        var returnedRequest = ccdClient.startCaseCreationTransfer(accessToken, etCCD);
        log.info("Creating case in {} for ET case {}", caseTypeId, caseId);
        ccdClient.submitCaseCreation(accessToken, etCCD, returnedRequest);
    }

    private CaseDetails createCaseDetailsCaseTransfer(uk.gov.hmcts.et.common.model.ccd.CaseData caseData,
                                                      String caseId, String caseTypeId, String ccdGatewayBaseUrl,
                                                      String positionTypeCT, String jurisdiction, String state,
                                                      String reasonForCT) {
        var newCaseDetails = new CaseDetails();
        newCaseDetails.setCaseTypeId(caseTypeId);
        newCaseDetails.setJurisdiction(jurisdiction);

        var newCaseData = generateNewCaseDataForCaseTransfer(caseData, caseId, ccdGatewayBaseUrl, positionTypeCT,
                                                                  state, reasonForCT);

        newCaseData.setReasonForCT(reasonForCT);
        newCaseDetails.setCaseData(newCaseData);
        return newCaseDetails;
    }

    private CaseData generateNewCaseDataForCaseTransfer(uk.gov.hmcts.et.common.model.ccd.CaseData caseData,
                                                        String caseId, String ccdGatewayBaseUrl, String positionTypeCT,
                                                        String state, String reasonForCT) {
        log.info("Copying case data for case {}", caseId);
        return TransferToEcmCaseDataHelper.copyCaseData(caseData, new CaseData(),
                                                        caseId, ccdGatewayBaseUrl, positionTypeCT, state, reasonForCT);
    }
}
