package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
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

    private String authToken = "Bearer some-random-token";

    @Test
    public void transferToEcm() throws IOException {
        var ethosCaseReference = "4150001/2020";
        var managingOffice = TribunalOffice.MANCHESTER.getOfficeName();
        var caseData = new CaseData();
        caseData.setEthosCaseReference(ethosCaseReference);
        caseData.setManagingOffice(managingOffice);
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);

        var createUpdateMsg = Helper.transferToEcmMessage();
        createEcmSingleService.sendCreation(submitEvent, authToken, createUpdateMsg);
        verify(ccdClient).startCaseCreationTransfer(eq(authToken), any());
        verify(ccdClient).submitCaseCreation(eq(authToken), any(), any());
    }

}
