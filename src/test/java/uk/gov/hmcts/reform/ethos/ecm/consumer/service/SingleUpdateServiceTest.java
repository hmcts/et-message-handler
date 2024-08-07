package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.SendNotificationDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.CaseDetails;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.et.common.model.ccd.types.SendNotificationTypeMultiple;
import uk.gov.hmcts.et.common.model.multiples.MultipleData;
import uk.gov.hmcts.et.common.model.multiples.SubmitMultipleEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ACCEPTED_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.MULTIPLE_CASE_TYPE;

@ExtendWith(SpringExtension.class)
class SingleUpdateServiceTest {

    @InjectMocks
    private transient SingleUpdateService singleUpdateService;
    @Mock
    private transient CcdClient ccdClient;

    private transient SubmitEvent submitEvent;
    private transient List<SubmitMultipleEvent> submitMultipleEvents;
    private transient UpdateCaseMsg updateCaseMsg;
    private transient String userToken;
    private CCDRequest returnedRequest;

    @BeforeEach
    public void setUp() {
        submitEvent = new SubmitEvent();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference("4150002/2020");
        caseData.setEcmCaseType(MULTIPLE_CASE_TYPE);
        caseData.setMultipleReference("4150002");
        caseData.setMultipleReferenceLinkMarkUp("MultipleReferenceLinkMarkUp");
        submitEvent.setCaseData(caseData);
        submitEvent.setState(ACCEPTED_STATE);

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseData(caseData);
        returnedRequest = new CCDRequest();
        returnedRequest.setCaseDetails(caseDetails);

        SubmitMultipleEvent submitMultipleEvent = new SubmitMultipleEvent();
        MultipleData multipleData = new MultipleData();
        multipleData.setMultipleReference("4150002");
        submitMultipleEvent.setCaseData(multipleData);
        submitMultipleEvent.setCaseId(1_649_258_182_799_287L);
        submitMultipleEvents = new ArrayList<>(Collections.singletonList(submitMultipleEvent));

        updateCaseMsg = Helper.generateUpdateCaseMsg();
        userToken = "accessToken";
    }

    @Test
    void sendUpdate() throws IOException {
        when(ccdClient.startEventForCaseAPIRole(anyString(), anyString(), anyString(), any()))
            .thenReturn(returnedRequest);
        when(ccdClient.submitEventForCase(anyString(), any(), anyString(), anyString(), any(), anyString()))
            .thenReturn(submitEvent);
        singleUpdateService.sendUpdate(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).startEventForCaseAPIRole(
            eq(userToken),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any()
        );
        verify(ccdClient).submitEventForCase(
            eq(userToken),
            any(),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any(),
            any()
        );
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    void sendPreAcceptToSingleLogic() throws IOException {
        updateCaseMsg = Helper.generatePreAcceptCaseMsg();
        when(ccdClient.startEventForCasePreAcceptBulkSingle(anyString(), anyString(), anyString(), any()))
            .thenReturn(returnedRequest);
        when(ccdClient.submitEventForCase(anyString(), any(), anyString(), anyString(), any(), anyString()))
            .thenReturn(submitEvent);
        singleUpdateService.sendUpdate(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).startEventForCasePreAcceptBulkSingle(
            eq(userToken),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any()
        );
        verify(ccdClient).submitEventForCase(
            eq(userToken),
            any(),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any(),
            any()
        );
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    void sendDisposeToSingleLogic() throws IOException {
        updateCaseMsg = Helper.generateCloseCaseMsg();
        when(ccdClient.startDisposeEventForCase(anyString(), anyString(), anyString(), any()))
            .thenReturn(returnedRequest);
        when(ccdClient.submitEventForCase(anyString(), any(), anyString(), anyString(), any(), anyString()))
            .thenReturn(submitEvent);
        singleUpdateService.sendUpdate(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).startDisposeEventForCase(
            eq(userToken),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any()
        );
        verify(ccdClient).submitEventForCase(
            eq(userToken),
            any(),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any(),
            any()
        );
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    void updateMultipleReferenceLinkMarkUp() throws IOException {
        submitEvent.getCaseData().setMultipleReferenceLinkMarkUp(null);
        when(ccdClient.startEventForCaseAPIRole(anyString(), anyString(), anyString(), any()))
            .thenReturn(returnedRequest);
        when(ccdClient.submitEventForCase(anyString(), any(), anyString(), anyString(), any(), anyString()))
            .thenReturn(submitEvent);
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(), anyString(), anyString()))
            .thenReturn(submitMultipleEvents);
        singleUpdateService.sendUpdate(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).startEventForCaseAPIRole(
            eq(userToken),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any()
        );
        verify(ccdClient).submitEventForCase(
            eq(userToken),
            any(),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any(),
            any()
        );
        verify(ccdClient).retrieveMultipleCasesElasticSearchWithRetries(
            eq(userToken),
            eq(updateCaseMsg.getCaseTypeId()),
            any()
        );
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    void sendUpdateForSendNotification() throws IOException {
        when(ccdClient.startEventForCase(anyString(), anyString(), anyString(), any(), anyString()))
            .thenReturn(returnedRequest);

        SendNotificationTypeMultiple sendNotification = new SendNotificationTypeMultiple();
        sendNotification.setSendNotificationNotify("Lead case");

        updateCaseMsg.setDataModelParent(SendNotificationDataModel.builder()
                                             .sendNotification(sendNotification)
                                             .build());

        singleUpdateService.sendUpdate(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).startEventForCase(
            eq(userToken),
            eq(UtilHelper.getCaseTypeId(updateCaseMsg.getCaseTypeId())),
            eq(updateCaseMsg.getJurisdiction()),
            any(),
            eq("sendNotificationMultiple")
        );
    }
}
