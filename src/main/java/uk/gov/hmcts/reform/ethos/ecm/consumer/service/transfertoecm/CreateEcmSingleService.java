package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    private final uk.gov.hmcts.ecm.common.client.CcdClient ccdClient;
    private final String officeName = TribunalOffice.SCOTLAND.getOfficeName();

    public void sendCreation(SubmitEvent oldSubmitEvent, String accessToken, CreateUpdatesMsg createUpdatesMsg)
        throws IOException {
        transferNewCase(oldSubmitEvent, createUpdatesMsg, accessToken);
    }

    private void transferNewCase(SubmitEvent oldSubmitEvent, CreateUpdatesMsg createUpdatesMsg, String accessToken)
        throws IOException {
        CaseDetails newEcmCaseDetailsCt = createCaseDetailsCaseTransfer(oldSubmitEvent, createUpdatesMsg);
        uk.gov.hmcts.ecm.common.model.ccd.CCDRequest returnedEcmCcdRequest =
            ccdClient.startEcmCaseCreationTransfer(accessToken, newEcmCaseDetailsCt);
        TransferToEcmDataModel transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        String officeCT = transferToEcmDataModel.getOfficeCT();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());
        log.info("Creating case in {} for ET case {}", officeCT, caseId);
        ccdClient.submitEcmCaseCreation(accessToken, newEcmCaseDetailsCt, returnedEcmCcdRequest);
    }

    private CaseDetails createCaseDetailsCaseTransfer(SubmitEvent oldSubmitEvent, CreateUpdatesMsg createUpdatesMsg) {
        TransferToEcmDataModel transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        String originalCaseTypeId = createUpdatesMsg.getCaseTypeId();
        String ccdGatewayBaseUrl = transferToEcmDataModel.getCcdGatewayBaseUrl();
        CaseData newEcmCaseData = generateNewCaseDataForCaseTransfer(oldSubmitEvent, ccdGatewayBaseUrl,
                                                                     originalCaseTypeId);
        newEcmCaseData.setReasonForCT(transferToEcmDataModel.getReasonForCT());
        CaseDetails newEcmCaseDetails = new CaseDetails();
        newEcmCaseDetails.setCaseData(newEcmCaseData);
        String officeCT = transferToEcmDataModel.getOfficeCT();
        String caseTypeId =  TribunalOffice.isScotlandOffice(officeCT) ? officeName : getCorrectedOfficeName(officeCT);
        newEcmCaseDetails.setCaseTypeId(caseTypeId);
        newEcmCaseDetails.setJurisdiction(createUpdatesMsg.getJurisdiction());
        return newEcmCaseDetails;
    }

    private CaseData generateNewCaseDataForCaseTransfer(SubmitEvent oldSubmitEvent, String ccdGatewayBaseUrl,
                                                        String caseTypeId) {
        uk.gov.hmcts.et.common.model.ccd.CaseData caseData = oldSubmitEvent.getCaseData();
        String state = oldSubmitEvent.getState();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());
        log.info("Copying case data for case {}", caseId);
        return TransferToEcmCaseDataHelper.copyCaseData(caseData, new CaseData(), caseId, ccdGatewayBaseUrl, state,
                                                        caseTypeId);
    }

    private static String getCorrectedOfficeName(String oldCaseOfficeName) {
        if (StringUtils.hasLength(oldCaseOfficeName) && oldCaseOfficeName.contains(" ")) {
            return oldCaseOfficeName.replace(" ", "");
        }
        return oldCaseOfficeName;
    }

}
