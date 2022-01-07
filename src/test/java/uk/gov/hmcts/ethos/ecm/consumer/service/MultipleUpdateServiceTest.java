package uk.gov.hmcts.ethos.ecm.consumer.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ecm.common.model.multiples.SubmitMultipleEvent;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.ethos.ecm.consumer.helpers.Helper;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.MultipleErrors;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.MultipleUpdateService;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ENGLANDWALES_BULK_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.MIGRATION_CASE_SOURCE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OPEN_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.TRANSFERRED_STATE;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.UNPROCESSABLE_STATE;

@RunWith(SpringJUnit4ClassRunner.class)
public class MultipleUpdateServiceTest {

    @InjectMocks
    private transient MultipleUpdateService multipleUpdateService;
    @Mock
    private transient CcdClient ccdClient;
    @Mock
    private transient UserService userService;
    @Captor
    private transient ArgumentCaptor<MultipleData> multipleDataArgumentCaptor;
    @Captor
    private transient ArgumentCaptor<MultipleData> creationMultipleDataArgumentCaptor;


    private transient List<SubmitMultipleEvent> submitMultipleEvents;
    private transient SubmitMultipleEvent submitMultipleEvent;
    private transient UpdateCaseMsg updateCaseMsg;
    private transient final String userToken = "Token";

    @Before
    public void setUp() {
        submitMultipleEvent = new SubmitMultipleEvent();
        MultipleData multipleData = new MultipleData();
        multipleData.setMultipleReference("4100001");
        submitMultipleEvent.setCaseData(multipleData);
        submitMultipleEvents = new ArrayList<>(Collections.singletonList(submitMultipleEvent));
        updateCaseMsg = Helper.generateUpdateCaseMsg();

        when(userService.getAccessToken()).thenReturn(userToken);
    }

    @Test
    public void testMultipleTransferSameCountry() throws IOException {
        updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        ((CreationSingleDataModel)updateCaseMsg.getDataModelParent()).setTransferSameCountry(true);
        var caseId = "12345";
        submitMultipleEvent.setCaseId(Long.parseLong(caseId));

        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                     updateCaseMsg.getMultipleRef()))
            .thenReturn(submitMultipleEvents);
        var ccdRequest = new CCDRequest();
        when(ccdClient.startBulkAmendEventForCase(userToken, updateCaseMsg.getCaseTypeId(),
                                                  updateCaseMsg.getJurisdiction(), caseId)).thenReturn(ccdRequest);


        var multipleErrorsList = new ArrayList<MultipleErrors>();

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, multipleErrorsList);

        assertTrue(multipleErrorsList.isEmpty());
        verify(ccdClient).startBulkAmendEventForCase(userToken, updateCaseMsg.getCaseTypeId(),
                                                     updateCaseMsg.getJurisdiction(), caseId);
        verify(ccdClient).submitMultipleEventForCase(eq(userToken),
                                                     multipleDataArgumentCaptor.capture(),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()), eq(ccdRequest), eq(caseId));
        var actualMultipleData = multipleDataArgumentCaptor.getValue();
        assertEquals(OPEN_STATE, actualMultipleData.getState());
        assertNull(actualMultipleData.getLinkedMultipleCT());

        verify(ccdClient, never()).startCaseMultipleCreation(anyString(), anyString(), anyString());
    }

    @Test
    public void testMultipleTransferDifferentCountry() throws IOException {
        updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        var newManagingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        var dataModel = (CreationSingleDataModel)updateCaseMsg.getDataModelParent();
        dataModel.setOfficeCT(newManagingOffice);
        dataModel.setTransferSameCountry(false);
        var caseId = "12345";
        submitMultipleEvent.setCaseId(Long.parseLong(caseId));

        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                     updateCaseMsg.getMultipleRef()))
            .thenReturn(submitMultipleEvents);
        var ccdRequest = new CCDRequest();
        when(ccdClient.startBulkAmendEventForCase(userToken, updateCaseMsg.getCaseTypeId(),
                                                  updateCaseMsg.getJurisdiction(), caseId)).thenReturn(ccdRequest);

        var creationCCDRequest = new CCDRequest();
        when(ccdClient.startCaseMultipleCreation(userToken, ENGLANDWALES_BULK_CASE_TYPE_ID,
                                                 updateCaseMsg.getJurisdiction())).thenReturn(creationCCDRequest);

        var multipleErrorsList = new ArrayList<MultipleErrors>();

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, multipleErrorsList);

        assertTrue(multipleErrorsList.isEmpty());
        verify(ccdClient).startBulkAmendEventForCase(userToken, updateCaseMsg.getCaseTypeId(),
                                                     updateCaseMsg.getJurisdiction(), caseId);
        verify(ccdClient).submitMultipleEventForCase(eq(userToken),
                                                     multipleDataArgumentCaptor.capture(),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()), eq(ccdRequest), eq(caseId));
        var actualMultipleData = multipleDataArgumentCaptor.getValue();
        assertEquals(TRANSFERRED_STATE, actualMultipleData.getState());
        assertEquals("Transferred to " + newManagingOffice, actualMultipleData.getLinkedMultipleCT());
        assertEquals(dataModel.getReasonForCT(), actualMultipleData.getReasonForCT());
        assertEquals(dataModel.getPositionTypeCT(), actualMultipleData.getPositionType());

        verify(ccdClient, times(1)).submitMultipleCreation(eq(userToken), creationMultipleDataArgumentCaptor.capture(),
                                                           eq(ENGLANDWALES_BULK_CASE_TYPE_ID),
                                                           eq(updateCaseMsg.getJurisdiction()),
                                                           eq(creationCCDRequest));
        actualMultipleData = creationMultipleDataArgumentCaptor.getValue();
        assertEquals(newManagingOffice, actualMultipleData.getManagingOffice());
        assertEquals(updateCaseMsg.getCaseTypeId(), actualMultipleData.getLinkedMultipleCT());
        assertEquals(MIGRATION_CASE_SOURCE, actualMultipleData.getMultipleSource());
        assertEquals(updateCaseMsg.getMultipleRef(), actualMultipleData.getMultipleReference());
    }

    @Test
    public void sendUpdateToMultipleLogic() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(),
                                                                     anyString(),
                                                                     anyString())).thenReturn(submitMultipleEvents);

        when(ccdClient.submitMultipleEventForCase(anyString(), any(), anyString(),
                                                  anyString(), any(), anyString())).thenReturn(submitMultipleEvent);
        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>());

        verifyMocks();
    }

    @Test
    public void sendUpdateToMultipleLogicEmptyES() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(),
                                                                     anyString(),
                                                                     anyString())).thenReturn(new ArrayList<>());

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>());

        verify(ccdClient).retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                        updateCaseMsg.getMultipleRef());
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    public void sendUpdateToMultipleLogicNullES() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(), anyString(),
                                                                     anyString())).thenReturn(null);

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>());

        verify(ccdClient).retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                        updateCaseMsg.getMultipleRef());
        verifyNoMoreInteractions(ccdClient);
    }

    @Test
    public void sendUpdateToMultipleLogicWithErrors() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(),
                                                                     anyString(),
                                                                     anyString())).thenReturn(submitMultipleEvents);

        when(ccdClient.submitMultipleEventForCase(anyString(), any(),
                                                  anyString(), anyString(), any(),
                                                  anyString())).thenReturn(submitMultipleEvent);
        MultipleErrors multipleErrors = new MultipleErrors();
        multipleErrors.setDescription(UNPROCESSABLE_STATE);
        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>(Collections.singletonList(
            multipleErrors)));

        verifyMocks();
    }

    @Test
    public void sendUpdateToMultipleTransferredLogic() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(),
                                                                     anyString(),
                                                                     anyString())).thenReturn(submitMultipleEvents);

        updateCaseMsg = Helper.generateCreationSingleCaseMsg();

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>());

        verify(ccdClient).retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                        updateCaseMsg.getMultipleRef());
        verify(ccdClient).startBulkAmendEventForCase(eq(userToken),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()),
                                                     any());
        verify(ccdClient).submitMultipleEventForCase(eq(userToken),
                                                     any(),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()),
                                                     any(),
                                                     any());

    }

    @Test(expected = Exception.class)
    public void sendUpdateToMultipleLogicException() throws IOException {
        when(ccdClient.retrieveMultipleCasesElasticSearchWithRetries(anyString(), anyString(),
                                                                     anyString())).thenThrow(new Exception());

        multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg, new ArrayList<>());
    }

    private void verifyMocks() throws IOException {
        verify(ccdClient).retrieveMultipleCasesElasticSearchWithRetries(userToken, updateCaseMsg.getCaseTypeId(),
                                                                        updateCaseMsg.getMultipleRef());
        verify(ccdClient).startBulkAmendEventForCase(eq(userToken),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()),
                                                     any());
        verify(ccdClient).submitMultipleEventForCase(eq(userToken),
                                                     any(),
                                                     eq(updateCaseMsg.getCaseTypeId()),
                                                     eq(updateCaseMsg.getJurisdiction()),
                                                     any(),
                                                     any());
        verifyNoMoreInteractions(ccdClient);
    }
}
