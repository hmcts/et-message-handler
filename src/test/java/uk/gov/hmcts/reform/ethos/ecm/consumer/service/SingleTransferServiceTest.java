package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.client.CcdSubmitEventParams;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.et.common.model.ccd.CCDRequest;
import uk.gov.hmcts.et.common.model.ccd.CaseData;
import uk.gov.hmcts.et.common.model.ccd.SubmitEvent;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ENGLANDWALES_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.SingleTransferService.SAME_COUNTRY_EVENT_SUMMARY_TEMPLATE;

@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings({"PMD.NcssCount", "PMD.LawOfDemeter"})
public class SingleTransferServiceTest {

    @InjectMocks
    private transient SingleTransferService singleTransferService;
    @Mock
    private transient CcdClient ccdClient;
    @Mock
    private transient SingleCreationService singleCreationService;
    @Captor
    private transient ArgumentCaptor<CcdSubmitEventParams> ccdSubmitEventParamsArgumentCaptor;

    @Test
    public void testTransferSameCountry() throws IOException {
        String caseTypeId = ENGLANDWALES_CASE_TYPE_ID;
        String jurisdiction = "EMPLOYMENT";
        String officeCT = TribunalOffice.NEWCASTLE.getOfficeName();
        String reasonCT = "A test transfer";
        String sourceEthosCaseReference = "12345/2021";
        UpdateCaseMsg updateCaseMsg = createUpdateCaseMsg(caseTypeId,
                                                          jurisdiction,
                                                          officeCT,
                                                          reasonCT,
                                                          sourceEthosCaseReference,
                                                          true);
        String caseId = "12345";
        SubmitEvent submitEvent = createSubmitEvent(caseId);

        String userToken = "my-test-token";
        CCDRequest ccdRequest = new CCDRequest();
        when(ccdClient.startCaseTransferSameCountryEccLinkedCase(userToken, caseTypeId, jurisdiction, caseId))
            .thenReturn(ccdRequest);

        singleTransferService.sendTransferred(submitEvent, userToken, updateCaseMsg);

        verify(ccdClient, times(1)).startCaseTransferSameCountryEccLinkedCase(userToken, caseTypeId, jurisdiction,
                                                                              caseId);
        verify(ccdClient, times(1)).submitEventForCase(ccdSubmitEventParamsArgumentCaptor.capture());
        var actualParams = ccdSubmitEventParamsArgumentCaptor.getValue();
        assertEquals(userToken, actualParams.getAuthToken());
        assertEquals(caseId, actualParams.getCaseId());
        assertEquals(caseTypeId, actualParams.getCaseTypeId());
        assertEquals(jurisdiction, actualParams.getJurisdiction());
        assertEquals(ccdRequest, actualParams.getCcdRequest());
        var expectedEventSummary = String.format(SAME_COUNTRY_EVENT_SUMMARY_TEMPLATE, officeCT,
                                                 sourceEthosCaseReference);
        assertEquals(expectedEventSummary, actualParams.getEventSummary());
        assertEquals(reasonCT, actualParams.getEventDescription());

        verifyNoInteractions(singleCreationService);
    }

    @Test
    public void testTransferDifferentCountry() throws IOException {
        var submitEvent = new SubmitEvent();
        var userToken = "my-test-token";
        var jurisdiction = "EMPLOYMENT";
        var officeCT = TribunalOffice.GLASGOW.getOfficeName();
        var reasonCT = "A test transfer";
        var sourceEthosCaseReference = "12345/2021";
        var updateCaseMsg = createUpdateCaseMsg(ENGLANDWALES_CASE_TYPE_ID, jurisdiction, officeCT, reasonCT,
                                                sourceEthosCaseReference, false);

        singleTransferService.sendTransferred(submitEvent, userToken, updateCaseMsg);

        verify(singleCreationService, times(1)).sendCreation(submitEvent, userToken, updateCaseMsg);
        verifyNoInteractions(ccdClient);
    }

    private UpdateCaseMsg createUpdateCaseMsg(String caseTypeId, String jurisdiction, String officeCT, String reasonCT,
                                              String sourceEthosCaseReference, boolean transferSameCountry) {
        var creationDataModel = CreationSingleDataModel.builder()
            .transferSameCountry(transferSameCountry)
            .officeCT(officeCT)
            .reasonForCT(reasonCT)
            .sourceEthosCaseReference(sourceEthosCaseReference)
            .build();
        return UpdateCaseMsg.builder()
            .dataModelParent(creationDataModel)
            .caseTypeId(caseTypeId)
            .jurisdiction(jurisdiction)
            .multipleRef(SINGLE_CASE_TYPE)
            .build();
    }

    private SubmitEvent createSubmitEvent(String caseId) {
        var submitEvent = new SubmitEvent();
        submitEvent.setCaseId(Long.parseLong(caseId));
        var caseData = new CaseData();
        submitEvent.setCaseData(caseData);
        return submitEvent;
    }
}
