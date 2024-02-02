package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;
import java.io.IOException;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ACCEPTED_STATE;

@SuppressWarnings({"PMD.NcssCount", "PMD.LawOfDemeter", "PMD.UnnecessaryFullyQualifiedName"})
@RunWith(MockitoJUnitRunner.class)
public class CreateEcmSingleServiceTest {
    @Mock
    private transient CcdClient ccdClient;
    @InjectMocks
    private CreateEcmSingleService createEcmSingleService;

    private static final String TEST_AUTH_TOKEN = "test auth token";

    @Test
    public void transferToEcm() throws IOException {
        String ethosCaseReference = "4150001/2020";
        String managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        SubmitEvent submitEvent = new SubmitEvent();
        submitEvent.setState(ACCEPTED_STATE);
        submitEvent.setCaseData(caseData);
        var caseDetails = new uk.gov.hmcts.et.common.model.ccd.CaseDetails();
        caseDetails.setCaseData(caseData);
        caseDetails.setCaseTypeId("Leeds");
        CCDRequest ccdRequest = new CCDRequest();
        ccdRequest.setCaseDetails(caseDetails);

        var returnedEcmCcdRequest = new uk.gov.hmcts.ecm.common.model.ccd.CCDRequest();
        var ecmCaseDetails = new uk.gov.hmcts.ecm.common.model.ccd.CaseDetails();
        returnedEcmCcdRequest.setCaseDetails(ecmCaseDetails);
        when(ccdClient.startEcmCaseCreationTransfer(eq(TEST_AUTH_TOKEN),
                                                    any(uk.gov.hmcts.ecm.common.model.ccd.CaseDetails.class)))
            .thenReturn(returnedEcmCcdRequest);

        var ecmCaseData = new uk.gov.hmcts.ecm.common.model.ccd.CaseData();
        ecmCaseData.setEthosCaseReference(ethosCaseReference);
        ecmCaseData.setManagingOffice(managingOffice);
        ecmCaseData.setEthosCaseReference("18850001/2020");
        var ecmSubmitEvent = new uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent();
        ecmSubmitEvent.setCaseData(ecmCaseData);
        when(ccdClient.submitEcmCaseCreation(eq(TEST_AUTH_TOKEN),
                                             any(uk.gov.hmcts.ecm.common.model.ccd.CaseDetails.class),
                                             any(uk.gov.hmcts.ecm.common.model.ccd.CCDRequest.class)))
            .thenReturn(ecmSubmitEvent);
        when(ccdClient.startEventForCase(eq(TEST_AUTH_TOKEN), any(), any(), any())).thenReturn(ccdRequest);

        CreateUpdatesMsg createUpdateMsg = Helper.transferToEcmMessage();

        createEcmSingleService.sendCreation(submitEvent, TEST_AUTH_TOKEN, createUpdateMsg);

        verify(ccdClient, times(1)).startEcmCaseCreationTransfer(eq(TEST_AUTH_TOKEN),
                                          any(uk.gov.hmcts.ecm.common.model.ccd.CaseDetails.class));
        //verify(ccdClient, times(1)).startEventForCase(eq(TEST_AUTH_TOKEN), any(), any(), any());
        verify(ccdClient, times(1)).submitEventForCase(eq(TEST_AUTH_TOKEN), any(), any(), any(),
                                                       any(), any());
    }

    @Test
    public void transferToEcmForOfficeNameWithWhiteSpace() throws IOException {
        String ethosCaseReference = "3600001/2021";
        String managingOffice = TribunalOffice.LONDON_EAST.getOfficeName();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        caseData.setDocumentCollection(new ArrayList<>());
        caseData.setAddressLabelCollection(new ArrayList<>());
        SubmitEvent submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);
        CreateUpdatesMsg createUpdateMsg = Helper.transferToEcmMessageForLondonEast();
        createEcmSingleService.sendCreation(submitEvent, TEST_AUTH_TOKEN, createUpdateMsg);

        var ecmCaseDetails = new CaseDetails();
        ecmCaseDetails.setCaseTypeId(managingOffice.replace(" ", ""));
        var ecmCaseData = new uk.gov.hmcts.ecm.common.model.ccd.CaseData();
        ecmCaseDetails.setCaseData(ecmCaseData);

        ArgumentCaptor<CaseDetails> ccdRequestCaptor = ArgumentCaptor.forClass(CaseDetails.class);
        verify(ccdClient, times(1))
            .submitEcmCaseCreation(eq(TEST_AUTH_TOKEN), ccdRequestCaptor.capture(), any());
        assertEquals(ecmCaseDetails.getCaseTypeId(), ccdRequestCaptor.getValue().getCaseTypeId());
    }
}
