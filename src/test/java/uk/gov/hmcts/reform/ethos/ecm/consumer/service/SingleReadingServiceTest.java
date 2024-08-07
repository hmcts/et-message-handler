package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Helper;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ACCEPTED_STATE;

@ExtendWith(SpringExtension.class)
class SingleReadingServiceTest {

    @InjectMocks
    private transient SingleReadingService singleReadingService;
    @Mock
    private transient CcdClient ccdClient;
    @Mock
    private transient UserService userService;
    @Mock
    private transient SingleUpdateService singleUpdateService;
    @Mock
    private transient SingleTransferService singleTransferService;

    private transient List<SubmitEvent> submitEvents;
    private static final String USER_TOKEN = "my-test-token";

    @BeforeEach
    public void setUp() {
        SubmitEvent submitEvent = new SubmitEvent();
        CaseData caseData = new CaseData();
        caseData.setEthosCaseReference("4150002/2020");
        submitEvent.setCaseData(caseData);
        submitEvent.setState(ACCEPTED_STATE);
        submitEvents = List.of(submitEvent);
    }

    @Test
    void sendUpdateToSingleLogic() throws IOException {
        var updateCaseMsg = Helper.generateUpdateCaseMsg();
        when(userService.getAccessToken()).thenReturn(USER_TOKEN);
        when(ccdClient.retrieveCasesElasticSearch(anyString(), anyString(), anyList())).thenReturn(submitEvents);

        singleReadingService.sendUpdateToSingleLogic(updateCaseMsg);

        verify(singleUpdateService, times(1)).sendUpdate(submitEvents.get(0), USER_TOKEN, updateCaseMsg);
        verifyNoInteractions(singleTransferService);
    }

    @Test
    void sendTransferredToSingleLogic() throws IOException {
        var updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        when(userService.getAccessToken()).thenReturn(USER_TOKEN);
        when(ccdClient.retrieveCasesElasticSearch(anyString(), anyString(), anyList())).thenReturn(submitEvents);

        singleReadingService.sendUpdateToSingleLogic(updateCaseMsg);

        verify(singleTransferService).sendTransferred(submitEvents.get(0), USER_TOKEN, updateCaseMsg);
        verifyNoInteractions(singleUpdateService);
    }

    @Test
    void sendUpdateToSingleLogicNoCasesFound() throws IOException {
        var updateCaseMsg = Helper.generateCreationSingleCaseMsg();
        when(userService.getAccessToken()).thenReturn(USER_TOKEN);
        when(ccdClient.retrieveCasesElasticSearch(anyString(), anyString(), anyList())).thenReturn(null);

        singleReadingService.sendUpdateToSingleLogic(updateCaseMsg);

        verifyNoInteractions(singleTransferService);
        verifyNoInteractions(singleUpdateService);
    }
}
