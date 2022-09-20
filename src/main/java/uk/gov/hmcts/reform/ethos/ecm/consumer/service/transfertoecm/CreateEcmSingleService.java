package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.TransferToEcmDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm.TransferToEcmCaseDataHelper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Service
public class CreateEcmSingleService {

    private final CcdClient ccdClient;
    private final String officeName = TribunalOffice.SCOTLAND.getOfficeName();

    public void sendCreation(SubmitEvent oldSubmitEvent, String accessToken, CreateUpdatesMsg createUpdatesMsg)
        throws IOException {
        TransferToEcmDataModel transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        String caseTypeId =  TribunalOffice.isScotlandOffice(transferToEcmDataModel.getOfficeCT())
            ? officeName : getCorrectedOfficeName(transferToEcmDataModel.getOfficeCT());

        String reasonForCT = transferToEcmDataModel.getReasonForCT();
        String ccdGatewayBaseUrl = transferToEcmDataModel.getCcdGatewayBaseUrl();
        String jurisdiction = createUpdatesMsg.getJurisdiction();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());

        transferNewCase(oldSubmitEvent, caseId, caseTypeId, ccdGatewayBaseUrl, jurisdiction, accessToken, reasonForCT);
    }

    private void transferNewCase(SubmitEvent oldSubmitEvent, String caseId, String caseTypeId, String ccdGatewayBaseUrl,
                                 String jurisdiction, String accessToken, String reasonForCT)
        throws IOException {
        CaseDetails newCaseDetailsCt = createCaseDetailsCaseTransfer(oldSubmitEvent.getCaseData(), caseId, caseTypeId,
                                                             ccdGatewayBaseUrl, jurisdiction,
                                                             oldSubmitEvent.getState(), reasonForCT);

        uk.gov.hmcts.et.common.model.ccd.CaseDetails etCCD = (uk.gov.hmcts.et.common.model.ccd.CaseDetails)
            TransferToEcmCaseDataHelper.objectMapper(
            newCaseDetailsCt, uk.gov.hmcts.et.common.model.ccd.CaseDetails.class);

        CCDRequest returnedRequest = ccdClient.startCaseCreationTransfer(accessToken, etCCD);
        log.info("Creating case in {} for ET case {}", caseTypeId, caseId);
        ccdClient.submitCaseCreation(accessToken, etCCD, returnedRequest);
    }

    private CaseDetails createCaseDetailsCaseTransfer(uk.gov.hmcts.et.common.model.ccd.CaseData caseData,
                                                      String caseId, String caseTypeId, String ccdGatewayBaseUrl,
                                                      String jurisdiction, String state,
                                                      String reasonForCT) {
        CaseDetails newCaseDetails = new CaseDetails();
        newCaseDetails.setCaseTypeId(caseTypeId);
        newCaseDetails.setJurisdiction(jurisdiction);

        CaseData newCaseData = generateNewCaseDataForCaseTransfer(caseData, caseId, ccdGatewayBaseUrl,
                                                                  state);
        newCaseData.setReasonForCT(reasonForCT);
        newCaseDetails.setCaseData(newCaseData);
        return newCaseDetails;
    }

    private CaseData generateNewCaseDataForCaseTransfer(uk.gov.hmcts.et.common.model.ccd.CaseData caseData,
                                                        String caseId, String ccdGatewayBaseUrl,
                                                        String state) {
        log.info("Copying case data for case {}", caseId);
        return TransferToEcmCaseDataHelper.copyCaseData(caseData, new CaseData(),
                                                        caseId, ccdGatewayBaseUrl, state);
    }

    private static String getCorrectedOfficeName(String oldCaseOfficeName) {
        if (StringUtils.hasLength(oldCaseOfficeName) && oldCaseOfficeName.contains(" ")) {
            return oldCaseOfficeName.replace(" ", "");
        }
        return oldCaseOfficeName;
    }

}
