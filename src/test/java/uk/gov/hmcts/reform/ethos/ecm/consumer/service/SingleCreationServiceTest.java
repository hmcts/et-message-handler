package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.CaseDetails;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ENGLANDWALES_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.SingleCreationService.CREATE_CASE_EVENT_SUMMARY_TEMPLATE;

@SuppressWarnings("PMD.LawOfDemeter")
@RunWith(SpringJUnit4ClassRunner.class)
public class SingleCreationServiceTest {
    @InjectMocks
    private SingleCreationService singleCreationService;
    @Mock
    private CcdClient ccdClient;
    private static final String USER_TOKEN = "accessToken";

    @Test
    public void caseTransferToScotlandCreateCase() throws IOException {
        var ethosCaseReference = "4150002/2020";
        var managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        var caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);
        CaseData newCaseData = new CaseData();
        SubmitEvent newCaseSubmitEvent = new SubmitEvent();
        newCaseSubmitEvent.setCaseData(newCaseData);

        UpdateCaseMsg updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setOfficeCT(
            TribunalOffice.GLASGOW.getOfficeName());
        when(ccdClient.submitCaseCreation(eq(USER_TOKEN), any(), any(), any()))
            .thenReturn(newCaseSubmitEvent);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseData(caseData);
        CCDRequest updateCCDRequest = new CCDRequest();
        updateCCDRequest.setCaseDetails(caseDetails);
        when(ccdClient.startEventForCase(eq(USER_TOKEN), any(), any(), any()))
            .thenReturn(updateCCDRequest);
        when(ccdClient.retrieveCasesElasticSearch(eq(USER_TOKEN), any(), any()))
            .thenReturn(new ArrayList<>());
        when(ccdClient.startCaseCreationTransfer(eq(USER_TOKEN), any()))
            .thenReturn(updateCCDRequest);

        singleCreationService.sendCreation(submitEvent, USER_TOKEN, updateCaseMsg);

        verify(ccdClient, times(1))
            .retrieveCasesElasticSearch(USER_TOKEN, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference));
        verify(ccdClient, times(1)).startCaseCreationTransfer(eq(USER_TOKEN), any());
        var expectedEventSummary = String.format(CREATE_CASE_EVENT_SUMMARY_TEMPLATE, managingOffice);
        verify(ccdClient, times(1)).submitCaseCreation(eq(USER_TOKEN), any(), any(),
                                                       eq(expectedEventSummary));
       // verify(ccdClient, times(1)).startEventForCase(eq(USER_TOKEN), any(), any(), any());
        verify(ccdClient, times(1)).submitEventForCase(eq(USER_TOKEN), any(), any(), any(),
                                                       any(), any());
        verify(ccdClient, times(0)).returnCaseCreationTransfer(eq(USER_TOKEN), any(), any(),
                                                               any());
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    public void caseTransferToEnglandCreateCase() throws IOException {
        String ethosCaseReference = "4150002/2020";
        String managingOffice = TribunalOffice.DUNDEE.getOfficeName();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        SubmitEvent submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);

        UpdateCaseMsg updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setOfficeCT(
            TribunalOffice.NEWCASTLE.getOfficeName());

        singleCreationService.sendCreation(submitEvent, USER_TOKEN, updateCaseMsg);

        verify(ccdClient).retrieveCasesElasticSearch(USER_TOKEN, ENGLANDWALES_CASE_TYPE_ID,
                                                     List.of(ethosCaseReference));
        verify(ccdClient).startCaseCreationTransfer(eq(USER_TOKEN), any());
        var expectedEventSummary = String.format(CREATE_CASE_EVENT_SUMMARY_TEMPLATE, managingOffice);
        verify(ccdClient).submitCaseCreation(eq(USER_TOKEN), any(), any(), eq(expectedEventSummary));
        verify(ccdClient, times(0)).returnCaseCreationTransfer(eq(USER_TOKEN), any(), any(),
                                                               any());
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    public void caseTransferToScotlandUpdateExisting() throws IOException {
        var ethosCaseReference = "4150002/2020";
        var managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        var caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);
        var caseId = 100;
        submitEvent.setCaseId(caseId);

        var updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setOfficeCT(
            TribunalOffice.GLASGOW.getOfficeName());

        when(ccdClient.retrieveCasesElasticSearch(USER_TOKEN, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference)))
            .thenReturn(new ArrayList<>(Collections.singletonList(submitEvent)));

        singleCreationService.sendCreation(submitEvent, USER_TOKEN, updateCaseMsg);

        verify(ccdClient).retrieveCasesElasticSearch(USER_TOKEN, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference));
        verify(ccdClient).returnCaseCreationTransfer(USER_TOKEN, SCOTLAND_CASE_TYPE_ID, "EMPLOYMENT",
                                                     String.valueOf(caseId));
        verify(ccdClient).submitEventForCase(eq(USER_TOKEN), any(), eq(SCOTLAND_CASE_TYPE_ID), eq("EMPLOYMENT"), any(),
                                             eq(String.valueOf(caseId)));
        verify(ccdClient, times(0)).startCaseCreationTransfer(eq(USER_TOKEN), any());
        verifyNoMoreInteractions(ccdClient);
    }

}
