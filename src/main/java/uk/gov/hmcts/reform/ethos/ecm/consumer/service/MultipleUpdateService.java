package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ecm.common.model.multiples.SubmitMultipleEvent;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.MultipleErrors;

import java.io.IOException;
import java.util.List;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.ERRORED_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.MIGRATION_CASE_SOURCE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OPEN_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.TRANSFERRED_STATE;

@Slf4j
@Service
public class MultipleUpdateService {

    private final transient CcdClient ccdClient;
    private final transient UserService userService;

    @Autowired
    public MultipleUpdateService(CcdClient ccdClient, UserService userService) {
        this.ccdClient = ccdClient;
        this.userService = userService;
    }

    public void sendUpdateToMultipleLogic(UpdateCaseMsg updateCaseMsg, List<MultipleErrors> multipleErrorsList)
        throws IOException {
        String accessToken = userService.getAccessToken();

        List<SubmitMultipleEvent> submitMultipleEvents = retrieveMultipleCase(accessToken, updateCaseMsg);
        if (submitMultipleEvents != null && !submitMultipleEvents.isEmpty()) {
            if (updateCaseMsg.getDataModelParent() instanceof CreationSingleDataModel) {
                handleMultipleTransfer(submitMultipleEvents.get(0), updateCaseMsg, accessToken, multipleErrorsList);
            } else {
                sendUpdate(submitMultipleEvents.get(0), accessToken, updateCaseMsg, multipleErrorsList, OPEN_STATE);
            }
        } else {
            log.warn("No submit events found for {}", updateCaseMsg.getMultipleRef());
        }
    }

    private List<SubmitMultipleEvent> retrieveMultipleCase(String authToken,
                                                           UpdateCaseMsg updateCaseMsg) throws IOException {

        return ccdClient.retrieveMultipleCasesElasticSearchWithRetries(authToken,
                                                        updateCaseMsg.getCaseTypeId(),
                                                        updateCaseMsg.getMultipleRef());
    }

    private void handleMultipleTransfer(SubmitMultipleEvent submitMultipleEvent, UpdateCaseMsg updateCaseMsg,
                                        String accessToken, List<MultipleErrors> multipleErrorsList)
        throws IOException {
        CreationSingleDataModel creationSingleDataModel = (CreationSingleDataModel) updateCaseMsg.getDataModelParent();
        if (creationSingleDataModel.isTransferSameCountry()) {
            sendUpdate(submitMultipleEvent, accessToken, updateCaseMsg, multipleErrorsList, OPEN_STATE);
        } else {
            sendUpdate(submitMultipleEvent, accessToken, updateCaseMsg, multipleErrorsList, TRANSFERRED_STATE);
            sendMultipleCreation(accessToken, updateCaseMsg, multipleErrorsList);
        }
    }

    private void sendUpdate(SubmitMultipleEvent submitMultipleEvent, String accessToken, UpdateCaseMsg updateCaseMsg,
                            List<MultipleErrors> multipleErrorsList, String multipleState) throws IOException {

        var multipleReference = updateCaseMsg.getMultipleRef();
        log.info("Update multiple {} to transferred", multipleReference);

        String caseTypeId = updateCaseMsg.getCaseTypeId();
        String jurisdiction = updateCaseMsg.getJurisdiction();
        String caseId = String.valueOf(submitMultipleEvent.getCaseId());

        CCDRequest returnedRequest = ccdClient.startBulkAmendEventForCase(accessToken,
                                                                          caseTypeId,
                                                                          jurisdiction,
                                                                          caseId);

        var multipleData = new MultipleData();
        if (CollectionUtils.isNotEmpty(multipleErrorsList)) {
            log.info("Updating multiple {} STATE to {}", multipleReference, ERRORED_STATE);
            multipleData.setState(ERRORED_STATE);
        } else {
            if (multipleState.equals(TRANSFERRED_STATE)) {
                var dataModel = (CreationSingleDataModel)updateCaseMsg.getDataModelParent();
                var officeCT = dataModel.getOfficeCT();
                var reasonForCT = dataModel.getReasonForCT();
                var positionTypeCT = dataModel.getPositionTypeCT();
                multipleData.setLinkedMultipleCT("Transferred to " + officeCT);
                multipleData.setReasonForCT(reasonForCT);
                multipleData.setPositionType(positionTypeCT);
            }

            log.info("Updating multiple {} STATE to {}", multipleReference, multipleState);
            multipleData.setState(multipleState);
        }

        ccdClient.submitMultipleEventForCase(accessToken,
                                             multipleData,
                                             caseTypeId,
                                             jurisdiction,
                                             returnedRequest,
                                             caseId);
    }

    private void sendMultipleCreation(String accessToken, UpdateCaseMsg updateCaseMsg,
                                      List<MultipleErrors> multipleErrorsList) throws IOException {

        if (!CollectionUtils.isEmpty(multipleErrorsList)) {
            return;
        }

        var dataModel = (CreationSingleDataModel)updateCaseMsg.getDataModelParent();
        var multipleReference = updateCaseMsg.getMultipleRef();
        var managingOffice = dataModel.getOfficeCT();
        log.info("Create new multiple for transfer of {} to {}", multipleReference, managingOffice);

        String caseTypeId = TribunalOffice.getCaseTypeId(managingOffice);
        String jurisdiction = updateCaseMsg.getJurisdiction();
        var multipleData = new MultipleData();

        //Used to pull the information for the old multiple on the new multiple creation
        multipleData.setLinkedMultipleCT(updateCaseMsg.getCaseTypeId());

        multipleData.setMultipleSource(MIGRATION_CASE_SOURCE);
        multipleData.setMultipleReference(multipleReference);
        multipleData.setManagingOffice(managingOffice);
        String multipleCaseTypeId = UtilHelper.getBulkCaseTypeId(caseTypeId);

        CCDRequest returnedRequest = ccdClient.startCaseMultipleCreation(accessToken,
                                                                         multipleCaseTypeId,
                                                                         jurisdiction);

        ccdClient.submitMultipleCreation(accessToken,
                                         multipleData,
                                         multipleCaseTypeId,
                                         jurisdiction,
                                         returnedRequest);
    }
}
