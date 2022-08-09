package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.BulkPdfDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.CaseDetails;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CasePdfGenerationService {
    private final CcdClient ccdClient;
    private final UserService userService;

    public void generatePdfs(CreateUpdatesMsg createUpdatesMsg) throws IOException {
        if (!(createUpdatesMsg.getDataModelParent() instanceof BulkPdfDataModel)) {
            log.warn("Invalid model state for messageID {}", createUpdatesMsg.getMsgId());
            return;
        } else {
            log.info("Searching for cases {} needing pdf re-generation", createUpdatesMsg.getEthosCaseRefCollection());
        }

        String accessToken = userService.getAccessToken();

        List<SubmitEvent> submitEvents = ccdClient.retrieveCasesElasticSearch(
            accessToken, createUpdatesMsg.getCaseTypeId(), createUpdatesMsg.getEthosCaseRefCollection());

        if (!submitEvents.isEmpty()) {
            submitEvents.forEach(submitEvent -> {
                    BulkPdfDataModel dataModel = (BulkPdfDataModel) createUpdatesMsg.getDataModelParent();

                    try {
                        CaseDetails caseDetails =
                            createCaseDetails(dataModel.getCaseType(), dataModel.getJurisdiction(),
                                submitEvent.getCaseData(), String.valueOf(submitEvent.getCaseId()));
                        CCDRequest ccdRequest = ccdClient.startInitialConsideration(accessToken,
                            caseDetails);

                        ccdClient.submitEventForCase(accessToken, caseDetails.getCaseData(),
                            caseDetails.getCaseTypeId(), caseDetails.getJurisdiction(), ccdRequest,
                            caseDetails.getCaseId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
        }
    }

    private CaseDetails createCaseDetails(String caseTypeId, String jurisdiction, CaseData caseData, String caseId) {
        CaseDetails caseDetails = new CaseDetails();

        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setJurisdiction(jurisdiction);
        caseDetails.setCaseData(caseData);
        caseDetails.setCaseId(caseId);

        return caseDetails;
    }
}
