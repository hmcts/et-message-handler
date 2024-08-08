package uk.gov.hmcts.reform.ethos.ecm.consumer.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicValueType;
import uk.gov.hmcts.et.common.model.ccd.items.DocumentTypeItem;
import uk.gov.hmcts.et.common.model.ccd.items.JurCodesTypeItem;
import uk.gov.hmcts.et.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.et.common.model.ccd.types.ClaimantIndType;
import uk.gov.hmcts.et.common.model.ccd.types.DocumentType;
import uk.gov.hmcts.et.common.model.ccd.types.JurCodesType;
import uk.gov.hmcts.et.common.model.ccd.types.RespondentSumType;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm.TransferToEcmCaseDataHelper;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ACCEPTED_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.UNASSIGNED_OFFICE;

@SuppressWarnings("PMD.LawOfDemeter")
class TransferToEcmCaseDataHelperTest {
    private static final String TEST = "Test";
    public static final String CASE_ID = "caseId";
    public static final String CCD_GATEWAY_BASE_URL = "ccdGatewayBaseUrl";
    private CaseData ecmCaseData;
    private uk.gov.hmcts.et.common.model.ccd.CaseData etCaseData;

    @BeforeEach
    public void setup() {
        ecmCaseData = new CaseData();
        etCaseData = createEtCaseData();
    }

    @Test
    void testCopyCaseData() {
        TransferToEcmCaseDataHelper.copyCaseData(etCaseData, ecmCaseData, CASE_ID, CCD_GATEWAY_BASE_URL,
                                                 ACCEPTED_STATE, SCOTLAND_CASE_TYPE_ID);
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getManagingOffice());
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getAllocatedOffice());
        assertEquals(TEST, ecmCaseData.getFileLocationGlasgow());
        assertEquals(1, ecmCaseData.getJurCodesCollection().size());
        assertEquals(TEST, ecmCaseData.getJurCodesCollection().get(0).getValue().getJuridictionCodesList());
        assertEquals(1, ecmCaseData.getRespondentCollection().size());
        assertEquals(1, ecmCaseData.getDocumentCollection().size());
        assertEquals(TEST, ecmCaseData.getClerkResponsible());
    }

    @Test
    void checkUnassignedOffice() {
        etCaseData.setManagingOffice(UNASSIGNED_OFFICE);
        etCaseData.setAllocatedOffice(UNASSIGNED_OFFICE);
        TransferToEcmCaseDataHelper.copyCaseData(etCaseData, ecmCaseData, CASE_ID, CCD_GATEWAY_BASE_URL,
                                                 ACCEPTED_STATE, SCOTLAND_CASE_TYPE_ID);
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getManagingOffice());
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getAllocatedOffice());
    }

    @ParameterizedTest
    @MethodSource("claimantGender")
    void testClaimantGenderMapping(String claimantSex, String ecmGender) {
        ClaimantIndType claimantIndType = new ClaimantIndType();
        claimantIndType.setClaimantSex(claimantSex);
        etCaseData.setClaimantIndType(claimantIndType);
        TransferToEcmCaseDataHelper.copyCaseData(etCaseData, ecmCaseData, CASE_ID, CCD_GATEWAY_BASE_URL,
                                                 ACCEPTED_STATE, SCOTLAND_CASE_TYPE_ID);
        assertEquals(ecmGender, ecmCaseData.getClaimantIndType().getClaimantGender());

    }

    public static Stream<Arguments> claimantGender() {
        return Stream.of(
            Arguments.of("Male", "Male"),
            Arguments.of("Female", "Female"),
            Arguments.of("Prefer not to say", null)
        );
    }

    @ParameterizedTest
    @MethodSource("claimantTitle")
    void testClaimantTitleMapping(String etTitle, String ecmTitle) {
        ClaimantIndType claimantIndType = new ClaimantIndType();
        claimantIndType.setClaimantPreferredTitle(etTitle);
        etCaseData.setClaimantIndType(claimantIndType);
        TransferToEcmCaseDataHelper.copyCaseData(etCaseData, ecmCaseData, CASE_ID, CCD_GATEWAY_BASE_URL,
                                                 ACCEPTED_STATE, SCOTLAND_CASE_TYPE_ID);
        assertEquals(ecmTitle, ecmCaseData.getClaimantIndType().getClaimantTitle());

    }

    public static Stream<Arguments> claimantTitle() {
        return Stream.of(
            Arguments.of("Mr", "Mr"),
            Arguments.of("Mrs", "Mrs"),
            Arguments.of("Miss", "Miss"),
            Arguments.of("Ms", "Ms"),
            Arguments.of("Mx", "Mx"),
            Arguments.of("Dr", null)
        );
    }

    private uk.gov.hmcts.et.common.model.ccd.CaseData createEtCaseData() {
        uk.gov.hmcts.et.common.model.ccd.CaseData caseData = new uk.gov.hmcts.et.common.model.ccd.CaseData();
        caseData.setManagingOffice(TribunalOffice.GLASGOW.getOfficeName());
        caseData.setAllocatedOffice(TribunalOffice.GLASGOW.getOfficeName());
        caseData.setFileLocationGlasgow(DynamicFixedListType.of(DynamicValueType.create(TEST, TEST)));
        JurCodesType jurCodesType = new JurCodesType();
        jurCodesType.setJuridictionCodesList(TEST);
        jurCodesType.setJudgmentOutcome(TEST);
        JurCodesTypeItem jurCodeTypeItem = new JurCodesTypeItem();
        jurCodeTypeItem.setValue(jurCodesType);
        caseData.setJurCodesCollection(List.of(jurCodeTypeItem));

        var respondentSumType = new RespondentSumType();
        respondentSumType.setRespondentName("Test Name");
        var respondentSumTypeItem = new RespondentSumTypeItem();
        respondentSumTypeItem.setValue(respondentSumType);
        caseData.setRespondentCollection(List.of(respondentSumTypeItem));

        DocumentType documentType = new DocumentType();
        documentType.setTypeOfDocument("Test Doc");
        documentType.setOwnerDocument("Test Owner");
        DocumentTypeItem documentTypeItem = new DocumentTypeItem();
        documentTypeItem.setValue(documentType);
        caseData.setDocumentCollection(List.of(documentTypeItem));

        caseData.setClerkResponsible(DynamicFixedListType.of(DynamicValueType.create(TEST, TEST)));

        return caseData;
    }

}
