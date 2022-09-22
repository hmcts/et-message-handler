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
    private final TransferToEcmCaseDataHelper dataHelper;

    public void sendCreation(SubmitEvent oldSubmitEvent, String accessToken, CreateUpdatesMsg createUpdatesMsg)
        throws IOException {
        transferNewCase(oldSubmitEvent, createUpdatesMsg, accessToken);
    }

    private void transferNewCase(SubmitEvent oldSubmitEvent, CreateUpdatesMsg createUpdatesMsg, String accessToken)
        throws IOException {

        CaseDetails newEcmCaseDetailsCt = createCaseDetailsCaseTransfer(oldSubmitEvent, createUpdatesMsg);
        uk.gov.hmcts.et.common.model.ccd.CaseDetails etCaseDetails = (uk.gov.hmcts.et.common.model.ccd.CaseDetails)
            dataHelper.convertEcmToEtCaseDetails(newEcmCaseDetailsCt,
                                                 uk.gov.hmcts.et.common.model.ccd.CaseDetails.class);

        CCDRequest returnedRequest = ccdClient.startCaseCreationTransfer(accessToken, etCaseDetails);

        TransferToEcmDataModel transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        String officeCT = transferToEcmDataModel.getOfficeCT();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());
        log.info("Creating case in {} for ET case {}", officeCT, caseId);
        ccdClient.submitCaseCreation(accessToken, etCaseDetails, returnedRequest);
    }

    private CaseDetails createCaseDetailsCaseTransfer(SubmitEvent oldSubmitEvent, CreateUpdatesMsg createUpdatesMsg) {

        TransferToEcmDataModel transferToEcmDataModel = (TransferToEcmDataModel) createUpdatesMsg.getDataModelParent();
        String officeCT = transferToEcmDataModel.getOfficeCT();
        String reasonForCT = transferToEcmDataModel.getReasonForCT();
        String ccdGatewayBaseUrl = transferToEcmDataModel.getCcdGatewayBaseUrl();

        CaseData newEcmCaseData = generateNewCaseDataForCaseTransfer(oldSubmitEvent, ccdGatewayBaseUrl);
        newEcmCaseData.setReasonForCT(reasonForCT);
        CaseDetails newEcmCaseDetails = new CaseDetails();
        newEcmCaseDetails.setCaseData(newEcmCaseData);
        String caseTypeId =  TribunalOffice.isScotlandOffice(officeCT) ? officeName : getCorrectedOfficeName(officeCT);
        newEcmCaseDetails.setCaseTypeId(caseTypeId);
        newEcmCaseDetails.setJurisdiction(createUpdatesMsg.getJurisdiction());
        return newEcmCaseDetails;

    }

    private CaseData generateNewCaseDataForCaseTransfer(SubmitEvent oldSubmitEvent, String ccdGatewayBaseUrl) {

        uk.gov.hmcts.et.common.model.ccd.CaseData caseData = oldSubmitEvent.getCaseData();
        String state = oldSubmitEvent.getState();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());
        log.info("Copying case data for case {}", caseId);
        return TransferToEcmCaseDataHelper.copyCaseData(caseData, new CaseData(), caseId, ccdGatewayBaseUrl, state);

    }

    private static String getCorrectedOfficeName(String oldCaseOfficeName) {
        if (StringUtils.hasLength(oldCaseOfficeName) && oldCaseOfficeName.contains(" ")) {
            return oldCaseOfficeName.replace(" ", "");
        }
        return oldCaseOfficeName;
    }

}
