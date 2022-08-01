package uk.gov.hmcts.reform.ethos.ecm.consumer.helpers;

import org.junit.Test;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.et.common.model.bulk.types.DynamicValueType;
import uk.gov.hmcts.et.common.model.ccd.items.DocumentTypeItem;
import uk.gov.hmcts.et.common.model.ccd.items.JurCodesTypeItem;
import uk.gov.hmcts.et.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.et.common.model.ccd.types.DocumentType;
import uk.gov.hmcts.et.common.model.ccd.types.JurCodesType;
import uk.gov.hmcts.et.common.model.ccd.types.RespondentSumType;
import uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm.TransferToEcmCaseDataHelper;

import java.util.List;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("PMD")
public class TransferToEcmCaseDataHelperTest {

    @Test
    public void testCopyCaseData() {
        var ecmCaseData = new CaseData();
        var etCaseData = createEtCaseData();
        ecmCaseData = TransferToEcmCaseDataHelper.copyCaseData(etCaseData, ecmCaseData, "caseId", "ccdGatewayBsaeUrl",
                                                                "Accepted");
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getManagingOffice());
        assertEquals(TribunalOffice.GLASGOW.getOfficeName(), ecmCaseData.getAllocatedOffice());
        assertEquals("Test", ecmCaseData.getFileLocationGlasgow());
        assertEquals(1, ecmCaseData.getJurCodesCollection().size());
        assertEquals("Test", ecmCaseData.getJurCodesCollection().get(0).getValue().getJuridictionCodesList());
        assertEquals(1, ecmCaseData.getRespondentCollection().size());
        assertEquals(1, ecmCaseData.getDocumentCollection().size());
        assertEquals("Test", ecmCaseData.getClerkResponsible());
    }

    private uk.gov.hmcts.et.common.model.ccd.CaseData createEtCaseData() {
        var caseData = new uk.gov.hmcts.et.common.model.ccd.CaseData();
        caseData.setManagingOffice(TribunalOffice.GLASGOW.getOfficeName());
        caseData.setAllocatedOffice(TribunalOffice.GLASGOW.getOfficeName());
        caseData.setFileLocationGlasgow(DynamicFixedListType.of(DynamicValueType.create("Test", "Test")));
        var jurCodesType = new JurCodesType();
        jurCodesType.setJuridictionCodesList("Test");
        jurCodesType.setJudgmentOutcome("Test");
        var jurCodeTypeItem = new JurCodesTypeItem();
        jurCodeTypeItem.setValue(jurCodesType);
        caseData.setJurCodesCollection(List.of(jurCodeTypeItem));

        var respondentSumType = new RespondentSumType();
        respondentSumType.setRespondentName("Test Name");
        var respondentSumTypeItem = new RespondentSumTypeItem();
        respondentSumTypeItem.setValue(respondentSumType);
        caseData.setRespondentCollection(List.of(respondentSumTypeItem));

        var documentType = new DocumentType();
        documentType.setTypeOfDocument("Test Doc");
        documentType.setOwnerDocument("Test Owner");
        var documentTypeItem = new DocumentTypeItem();
        documentTypeItem.setValue(documentType);
        caseData.setDocumentCollection(List.of(documentTypeItem));

        caseData.setClerkResponsible(DynamicFixedListType.of(DynamicValueType.create("Test", "Test")));

        return caseData;
    }

}
