package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;
import java.io.IOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
public class CreateEcmSingleServiceTest {

    @Mock
    private CcdClient ccdClient;

    @InjectMocks
    private CreateEcmSingleService createEcmSingleService;

    @Test
    @SuppressWarnings({"PMD.NcssCount", "PMD.LawOfDemeter"})
    public void transferToEcm() throws IOException {
        String ethosCaseReference = "4150001/2020";
        String managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        SubmitEvent submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);

        CreateUpdatesMsg createUpdateMsg = Helper.transferToEcmMessage();
        String authToken = "Bearer some-random-token";
        createEcmSingleService.sendCreation(submitEvent, authToken, createUpdateMsg);
        verify(ccdClient).startCaseCreationTransfer(eq(authToken), any());
        verify(ccdClient).submitCaseCreation(eq(authToken), any(), any());
    }

}
