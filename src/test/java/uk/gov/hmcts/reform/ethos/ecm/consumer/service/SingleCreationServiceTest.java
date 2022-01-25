package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ENGLANDWALES_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.SingleCreationService.CREATE_CASE_EVENT_SUMMARY_TEMPLATE;

@RunWith(SpringJUnit4ClassRunner.class)
public class SingleCreationServiceTest {

    @InjectMocks
    private transient SingleCreationService singleCreationService;
    @Mock
    private transient CcdClient ccdClient;

    private final transient String userToken = "accessToken";

    @Test
    public void caseTransferToScotlandCreateCase() throws IOException {
        var ethosCaseReference = "4150002/2020";
        var managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        var caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);

        var updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setOfficeCT(
            TribunalOffice.GLASGOW.getOfficeName());

        singleCreationService.sendCreation(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).retrieveCasesElasticSearch(userToken, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference));
        verify(ccdClient).startCaseCreationTransfer(eq(userToken), any());
        var expectedEventSummary = String.format(CREATE_CASE_EVENT_SUMMARY_TEMPLATE, managingOffice);
        verify(ccdClient).submitCaseCreation(eq(userToken), any(), any(), eq(expectedEventSummary));
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    public void caseTransferToEnglandCreateCase() throws IOException {
        var ethosCaseReference = "4150002/2020";
        var managingOffice = TribunalOffice.DUNDEE.getOfficeName();
        var caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);

        var updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setOfficeCT(
            TribunalOffice.NEWCASTLE.getOfficeName());

        singleCreationService.sendCreation(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).retrieveCasesElasticSearch(userToken, ENGLANDWALES_CASE_TYPE_ID, List.of(ethosCaseReference));
        verify(ccdClient).startCaseCreationTransfer(eq(userToken), any());
        var expectedEventSummary = String.format(CREATE_CASE_EVENT_SUMMARY_TEMPLATE, managingOffice);
        verify(ccdClient).submitCaseCreation(eq(userToken), any(), any(), eq(expectedEventSummary));
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

        when(ccdClient.retrieveCasesElasticSearch(userToken, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference)))
            .thenReturn(new ArrayList<>(Collections.singletonList(submitEvent)));

        singleCreationService.sendCreation(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient).retrieveCasesElasticSearch(userToken, SCOTLAND_CASE_TYPE_ID, List.of(ethosCaseReference));
        verify(ccdClient).returnCaseCreationTransfer(userToken, SCOTLAND_CASE_TYPE_ID, "EMPLOYMENT",
                                                     String.valueOf(caseId));
        verify(ccdClient).submitEventForCase(eq(userToken), any(), eq(SCOTLAND_CASE_TYPE_ID), eq("EMPLOYMENT"), any(),
                                             eq(String.valueOf(caseId)));
        verifyNoMoreInteractions(ccdClient);
    }
}
