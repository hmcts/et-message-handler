package uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm;

import org.junit.Before;
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
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.UserService;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class TransferToEcmServiceTest {
    @Mock
    private CcdClient ccdClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransferToEcmService transferToEcmService;

    @Mock
    private CreateEcmSingleService createEcmSingleService;

    private static final String USER_TOKEN = "Bearer some-auth-token";
    private CreateUpdatesMsg createUpdatesMsg;
    private List<SubmitEvent> submitEventList;

    @Before
    @SuppressWarnings("PMD.LawOfDemeter")
    public void setUp() {
        createUpdatesMsg = Helper.transferToEcmMessage();
        var caseData = new CaseData();
        caseData.setManagingOffice(TribunalOffice.LEEDS.getOfficeName());
        caseData.setEthosCaseReference("6000000/2022");
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseData(caseData);
        submitEventList = List.of(submitEvent);

    }

    @Test
    public void transferToEcm() throws IOException {
        when(userService.getAccessToken())
            .thenReturn(USER_TOKEN);
        when(ccdClient.retrieveCasesElasticSearch(anyString(), anyString(), anyList()))
            .thenReturn(submitEventList);

        transferToEcmService.transferToEcm(createUpdatesMsg);
        verify(ccdClient, times(1))
            .retrieveCasesElasticSearch(USER_TOKEN, createUpdatesMsg.getCaseTypeId(),
                                        createUpdatesMsg.getEthosCaseRefCollection());
        verify(createEcmSingleService, times(1))
            .sendCreation(submitEventList.get(0), USER_TOKEN, createUpdatesMsg);
    }

}
