package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.CaseDetails;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm.SingleCreationServiceHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CLOSED_STATE;

@Slf4j
@RequiredArgsConstructor
@Service
public class SingleCreationService {

    public static final String CREATE_CASE_EVENT_SUMMARY_TEMPLATE = "Case created by transfer from %s";

    private final CcdClient ccdClient;
    private String ccdGatewayBaseUrl;

    public void sendCreation(SubmitEvent oldSubmitEvent, String accessToken, UpdateCaseMsg updateCaseMsg)
        throws IOException {

        CreationSingleDataModel creationSingleDataModel = (CreationSingleDataModel) updateCaseMsg.getDataModelParent();
        String caseTypeId = TribunalOffice.getCaseTypeId(creationSingleDataModel.getOfficeCT());
        String sourceCaseTypeId = updateCaseMsg.getCaseTypeId();
        ccdGatewayBaseUrl = creationSingleDataModel.getCcdGatewayBaseUrl();
        String jurisdiction = updateCaseMsg.getJurisdiction();
        String caseId = String.valueOf(oldSubmitEvent.getCaseId());
        String ethosCaseReference = oldSubmitEvent.getCaseData().getEthosCaseReference();

        SubmitEvent caseDestinationOffice = existCaseDestinationOffice(accessToken, ethosCaseReference, caseTypeId);

        if (caseDestinationOffice == null) {
            log.info("Creating new case for transfer to {} {}", caseTypeId, ethosCaseReference);
            transferNewCase(oldSubmitEvent, caseId, caseTypeId, ccdGatewayBaseUrl,
                            jurisdiction, accessToken, creationSingleDataModel, sourceCaseTypeId);
        } else {
            log.info("Case exists for transfer to {} {}", caseTypeId, ethosCaseReference);
            updateExistingCase(caseDestinationOffice, oldSubmitEvent, caseId, caseTypeId, jurisdiction, accessToken,
                               ccdGatewayBaseUrl, creationSingleDataModel
            );
        }
    }

    private void updateExistingCase(SubmitEvent caseDestinationOffice, SubmitEvent oldSubmitEvent,
                                    String caseId, String caseTypeId, String jurisdiction,
                                    String accessToken, String ccdGatewayBaseUrl,
                                    CreationSingleDataModel creationSingleDataModel)
        throws IOException {

        String destinationCaseId = String.valueOf(caseDestinationOffice.getCaseId());

        CCDRequest returnedRequest = ccdClient.returnCaseCreationTransfer(
            accessToken,
            caseTypeId,
            jurisdiction,
            destinationCaseId
        );

        ccdClient.submitEventForCase(
            accessToken,
            generateCaseDataCaseTransfer(caseDestinationOffice.getCaseData(),
                                         oldSubmitEvent.getCaseData(),
                                         caseId,
                                         ccdGatewayBaseUrl,
                                         creationSingleDataModel.getPositionTypeCT(),
                                         oldSubmitEvent.getState(),
                                         creationSingleDataModel.getOfficeCT(),
                                         creationSingleDataModel.getReasonForCT()
            ),
            caseTypeId,
            jurisdiction,
            returnedRequest,
            destinationCaseId
        );
    }

    private void transferNewCase(SubmitEvent oldSubmitEvent, String caseId,
                                 String caseTypeId, String ccdGatewayBaseUrl,
                                 String jurisdiction, String accessToken,
                                 CreationSingleDataModel creationSingleDataModel,
                                 String sourceCaseTypeId) throws IOException {
        CaseDetails newCaseDetailsCT = createCaseDetailsCaseTransfer(oldSubmitEvent.getCaseData(),
                                                                     caseId,
                                                                     caseTypeId,
                                                                     ccdGatewayBaseUrl,
                                                                     creationSingleDataModel.getPositionTypeCT(),
                                                                     jurisdiction,
                                                                     oldSubmitEvent.getState(),
                                                                     creationSingleDataModel.getOfficeCT(),
                                                                     creationSingleDataModel.getReasonForCT());
        CCDRequest returnedRequest = ccdClient.startCaseCreationTransfer(accessToken, newCaseDetailsCT);
        String eventSummary = String.format(CREATE_CASE_EVENT_SUMMARY_TEMPLATE,
                                         oldSubmitEvent.getCaseData().getManagingOffice());
        SubmitEvent newCase = ccdClient.submitCaseCreation(accessToken, newCaseDetailsCT, returnedRequest,
                                                           eventSummary);
        if (newCase != null) {
            String transferredCaseLink =
                SingleCreationServiceHelper.getTransferredCaseLink(ccdGatewayBaseUrl,
                                                                   String.valueOf(newCase.getCaseId()),
                                                                   newCase.getCaseData().getEthosCaseReference());
            CCDRequest updateCCDRequest = ccdClient.startEventForCase(accessToken, sourceCaseTypeId, jurisdiction,
                                                                      caseId);

            updateCCDRequest.getCaseDetails().getCaseData().setTransferredCaseLink(transferredCaseLink);
            ccdClient.submitEventForCase(accessToken, updateCCDRequest.getCaseDetails().getCaseData(), sourceCaseTypeId,
                                         jurisdiction, updateCCDRequest, caseId);
        }
    }

    private SubmitEvent existCaseDestinationOffice(String accessToken, String ethosCaseReference,
                                                   String destinationCaseTypeId) throws IOException {
        List<SubmitEvent> submitEvents = retrieveDestinationCase(accessToken, ethosCaseReference,
                                                                 destinationCaseTypeId);
        return submitEvents.isEmpty() ? null : submitEvents.get(0);
    }

    private List<SubmitEvent> retrieveDestinationCase(String authToken, String ethosCaseReference,
                                                      String destinationCaseTypeId) throws IOException {
        return ccdClient.retrieveCasesElasticSearch(authToken, destinationCaseTypeId,
                                                    new ArrayList<>(Collections.singletonList(ethosCaseReference)));
    }

    private CaseDetails createCaseDetailsCaseTransfer(CaseData oldCaseData, String caseId, String caseTypeId,
                                                      String ccdGatewayBaseUrl, String positionTypeCT,
                                                      String jurisdiction, String state, String owningOfficeCT,
                                                      String reasonForCT) {
        CaseDetails newCaseTransferCaseDetails = new CaseDetails();
        newCaseTransferCaseDetails.setCaseTypeId(caseTypeId);
        newCaseTransferCaseDetails.setJurisdiction(jurisdiction);

        CaseData newCaseData = generateNewCaseDataCaseTransfer(oldCaseData,
                                                               caseId,
                                                               ccdGatewayBaseUrl,
                                                               positionTypeCT,
                                                               state,
                                                               owningOfficeCT,
                                                               reasonForCT);
        newCaseData.setReasonForCT(reasonForCT);
        newCaseTransferCaseDetails.setCaseData(newCaseData);
        return newCaseTransferCaseDetails;
    }

    private CaseData generateNewCaseDataCaseTransfer(CaseData oldCaseData, String caseId,
                                                     String ccdGatewayBaseUrl, String positionTypeCT,
                                                     String state, String owningOfficeCT, String reasonForCT) {
        return copyCaseData(oldCaseData, new CaseData(), caseId, ccdGatewayBaseUrl, positionTypeCT, state,
                            owningOfficeCT, reasonForCT);
    }

    private CaseData generateCaseDataCaseTransfer(CaseData newCaseData, CaseData oldCaseData, String caseId,
                                                  String ccdGatewayBaseUrl, String positionTypeCT,
                                                  String state, String owningOfficeCT, String reasonForCT) {

        return copyCaseData(oldCaseData, newCaseData, caseId, ccdGatewayBaseUrl, positionTypeCT, state, owningOfficeCT,
                            reasonForCT);
    }

    private CaseData copyCaseData(CaseData oldCaseData, CaseData newCaseData, String caseId, String ccdGatewayBaseUrl,
                                  String positionTypeCT, String state, String owningOfficeCT, String reasonForCT) {
        newCaseData.setEthosCaseReference(oldCaseData.getEthosCaseReference());
        newCaseData.setEcmCaseType(oldCaseData.getEcmCaseType());
        newCaseData.setClaimantTypeOfClaimant(oldCaseData.getClaimantTypeOfClaimant());
        newCaseData.setClaimantCompany(oldCaseData.getClaimantCompany());
        newCaseData.setClaimantIndType(oldCaseData.getClaimantIndType());
        newCaseData.setClaimantType(oldCaseData.getClaimantType());
        newCaseData.setClaimantOtherType(oldCaseData.getClaimantOtherType());
        newCaseData.setPreAcceptCase(oldCaseData.getPreAcceptCase());
        newCaseData.setReceiptDate(oldCaseData.getReceiptDate());
        newCaseData.setFeeGroupReference(oldCaseData.getFeeGroupReference());
        newCaseData.setClaimantWorkAddressQuestion(oldCaseData.getClaimantWorkAddressQuestion());
        newCaseData.setClaimantWorkAddressQRespondent(oldCaseData.getClaimantWorkAddressQRespondent());
        newCaseData.setRepresentativeClaimantType(oldCaseData.getRepresentativeClaimantType());
        newCaseData.setRespondentCollection(oldCaseData.getRespondentCollection());
        newCaseData.setRepCollection(oldCaseData.getRepCollection());
        newCaseData.setPositionType(oldCaseData.getPositionTypeCT());
        newCaseData.setDateToPosition(oldCaseData.getDateToPosition());
        newCaseData.setCurrentPosition(oldCaseData.getCurrentPosition());
        newCaseData.setDepositCollection(oldCaseData.getDepositCollection());
        newCaseData.setJudgementCollection(oldCaseData.getJudgementCollection());
        newCaseData.setJurCodesCollection(oldCaseData.getJurCodesCollection());
        newCaseData.setBfActions(oldCaseData.getBfActions());
        newCaseData.setUserLocation(oldCaseData.getUserLocation());
        newCaseData.setDocumentCollection(oldCaseData.getDocumentCollection());
        newCaseData.setAdditionalCaseInfoType(oldCaseData.getAdditionalCaseInfoType());
        newCaseData.setCaseNotes(oldCaseData.getCaseNotes());
        newCaseData.setClaimantWorkAddress(oldCaseData.getClaimantWorkAddress());
        newCaseData.setClaimantRepresentedQuestion(oldCaseData.getClaimantRepresentedQuestion());
        newCaseData.setCaseSource(oldCaseData.getCaseSource());
        newCaseData.setConciliationTrack(oldCaseData.getConciliationTrack());
        newCaseData.setCounterClaim(oldCaseData.getCounterClaim());
        newCaseData.setEccCases(oldCaseData.getEccCases());
        newCaseData.setRestrictedReporting(oldCaseData.getRestrictedReporting());
        newCaseData.setRespondent(oldCaseData.getRespondent());
        newCaseData.setClaimant(oldCaseData.getClaimant());
        newCaseData.setCaseRefECC(oldCaseData.getCaseRefECC());
        newCaseData.setCcdID(oldCaseData.getCcdID());
        newCaseData.setFlagsImageAltText(oldCaseData.getFlagsImageAltText());
        newCaseData.setCompanyPremises(oldCaseData.getCompanyPremises());
        if (state != null && !state.equals(CLOSED_STATE)) {
            newCaseData.setPositionType(positionTypeCT);
        }
        newCaseData.setManagingOffice(owningOfficeCT);
        newCaseData.setMultipleReference(oldCaseData.getMultipleReference());
        log.info("setLeadClaimant is set to " + oldCaseData.getLeadClaimant());
        newCaseData.setLeadClaimant(oldCaseData.getLeadClaimant());

        newCaseData.setReasonForCT(reasonForCT);
        newCaseData.setLinkedCaseCT(generateMarkUp(ccdGatewayBaseUrl, caseId, oldCaseData.getEthosCaseReference()));
        return newCaseData;
    }

    private String generateMarkUp(String ccdGatewayBaseUrl, String caseId, String ethosCaseRef) {
        String url = ccdGatewayBaseUrl + "/cases/case-details/" + caseId;
        return "<a target=\"_blank\" href=\"" + url + "\">" + ethosCaseRef + "</a>";
    }
}
