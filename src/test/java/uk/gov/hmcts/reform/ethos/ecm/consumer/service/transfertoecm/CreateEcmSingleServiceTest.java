package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.CaseDetails;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;
import java.io.IOException;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"PMD.NcssCount", "PMD.LawOfDemeter"})
@RunWith(SpringJUnit4ClassRunner.class)
public class CreateEcmSingleServiceTest {

    @Mock
    private CcdClient ccdClient;

    @Mock
    private CCDRequest ccdRequest;

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
        submitEvent.setCaseData(caseData);

        CreateUpdatesMsg createUpdateMsg = Helper.transferToEcmMessage();
        createEcmSingleService.sendCreation(submitEvent, TEST_AUTH_TOKEN, createUpdateMsg);
        verify(ccdClient).startCaseCreationTransfer(eq(TEST_AUTH_TOKEN), any());
        verify(ccdClient).submitCaseCreation(eq(TEST_AUTH_TOKEN), any(), any());
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

        // Construct CaseDetails object provided as an argument in internal method call.
        // The object has CaseTypeId field that is being tested in this unit test
        CaseDetails etCaseDetails = new CaseDetails();
        etCaseDetails.setCaseData(caseData);
        etCaseDetails.setCaseTypeId(managingOffice.replace(" ", ""));

        String transferredCaseLink = "<a target=\"_blank\" "
            + "href=\"ccdGatewayBaseUrl/cases/case-details/0\">"
            + ethosCaseReference + "</a>";
        caseData.setLinkedCaseCT(transferredCaseLink);
        etCaseDetails.setCaseData(caseData);
        ccdRequest.setCaseDetails(etCaseDetails);
        CreateUpdatesMsg createUpdateMsg = Helper.transferToEcmMessageForLondonEast();
        createEcmSingleService.sendCreation(submitEvent, TEST_AUTH_TOKEN, createUpdateMsg);

        // Resetting EthosCaseReference is needed as this is RET to ECM case transfer
        etCaseDetails.getCaseData().setEthosCaseReference(null);

        ArgumentCaptor<CaseDetails> captor =
            ArgumentCaptor.forClass(CaseDetails.class);
        verify(ccdClient).startCaseCreationTransfer(any(), captor.capture());
        assertEquals(etCaseDetails.getCaseTypeId(), captor.getValue().getCaseTypeId());
    }

}
