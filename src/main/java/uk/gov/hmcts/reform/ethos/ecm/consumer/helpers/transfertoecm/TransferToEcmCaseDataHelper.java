package uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.ecm.common.model.bulk.types.DynamicFixedListType;
import uk.gov.hmcts.ecm.common.model.ccd.Address;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.items.AddressLabelTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.DocumentTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.JurCodesTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.AdditionalCaseInfoType;
import uk.gov.hmcts.ecm.common.model.ccd.types.AddressLabelType;
import uk.gov.hmcts.ecm.common.model.ccd.types.AddressLabelsAttributesType;
import uk.gov.hmcts.ecm.common.model.ccd.types.AddressLabelsSelectionType;
import uk.gov.hmcts.ecm.common.model.ccd.types.CasePreAcceptType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantIndType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantOtherType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantType;
import uk.gov.hmcts.ecm.common.model.ccd.types.ClaimantWorkAddressType;
import uk.gov.hmcts.ecm.common.model.ccd.types.CompanyPremisesType;
import uk.gov.hmcts.ecm.common.model.ccd.types.CorrespondenceScotType;
import uk.gov.hmcts.ecm.common.model.ccd.types.CorrespondenceType;
import uk.gov.hmcts.ecm.common.model.ccd.types.DocumentType;
import uk.gov.hmcts.ecm.common.model.ccd.types.JurCodesType;
import uk.gov.hmcts.ecm.common.model.ccd.types.RepresentedTypeC;
import uk.gov.hmcts.ecm.common.model.ccd.types.RespondentSumType;
import uk.gov.hmcts.ecm.common.model.ccd.types.RestrictedReportingType;
import uk.gov.hmcts.ecm.common.model.ccd.types.UploadedDocumentType;
import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CLOSED_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.POSITION_TYPE_CASE_TRANSFERRED_OTHER_COUNTRY;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.UNASSIGNED_OFFICE;

@Slf4j
@SuppressWarnings({"PMD.NcssCount", "PMD.CouplingBetweenObjects", "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.ExcessiveMethodLength", "PMD.LawOfDemeter"})
public final class TransferToEcmCaseDataHelper {
    private TransferToEcmCaseDataHelper() {
        // Access through static methods
    }

    public static CaseData copyCaseData(uk.gov.hmcts.et.common.model.ccd.CaseData oldCaseData, CaseData caseData,
                                        String caseId, String ccdGatewayBaseUrl, String state, String caseTypeId) {
        ObjectMapper mapper = new ObjectMapper();
        caseData.setEcmCaseType(oldCaseData.getEcmCaseType());
        caseData.setTribunalCorrespondenceAddress((Address) objectMapper(
            oldCaseData.getTribunalCorrespondenceAddress(), Address.class, mapper));
        caseData.setTribunalCorrespondenceTelephone(oldCaseData.getTribunalCorrespondenceTelephone());
        caseData.setTribunalCorrespondenceFax(oldCaseData.getTribunalCorrespondenceFax());
        caseData.setTribunalCorrespondenceDX(oldCaseData.getTribunalCorrespondenceDX());
        caseData.setTribunalCorrespondenceEmail(oldCaseData.getTribunalCorrespondenceEmail());
        caseData.setClaimantTypeOfClaimant(oldCaseData.getClaimantTypeOfClaimant());
        caseData.setClaimantCompany(oldCaseData.getClaimantCompany());
        caseData.setClaimantIndType(mapClaimantIndType(oldCaseData, mapper));
        caseData.setClaimantType((ClaimantType) objectMapper(oldCaseData.getClaimantType(),
                                                                        ClaimantType.class, mapper));
        caseData.setClaimantOtherType((ClaimantOtherType) objectMapper(
            oldCaseData.getClaimantOtherType(), ClaimantOtherType.class, mapper));
        caseData.setPreAcceptCase((CasePreAcceptType) objectMapper(
            oldCaseData.getClaimantType(), CasePreAcceptType.class, mapper));
        caseData.setReceiptDate(oldCaseData.getReceiptDate());
        caseData.setClaimServedDate(oldCaseData.getClaimServedDate());
        caseData.setFeeGroupReference(oldCaseData.getFeeGroupReference());
        caseData.setClaimantWorkAddressQuestion(oldCaseData.getClaimantWorkAddressQuestion());
        caseData.setClaimantWorkAddressQRespondent((DynamicFixedListType) objectMapper(
            oldCaseData.getClaimantWorkAddressQRespondent(), DynamicFixedListType.class, mapper));
        caseData.setRepresentativeClaimantType((RepresentedTypeC) objectMapper(
            oldCaseData.getRepresentativeClaimantType(), RepresentedTypeC.class, mapper));
        caseData.setPositionType(oldCaseData.getPositionType());
        caseData.setDateToPosition(oldCaseData.getDateToPosition());
        caseData.setCurrentPosition(oldCaseData.getCurrentPosition());
        caseData.setUserLocation(oldCaseData.getUserLocation());
        caseData.setAdditionalCaseInfoType((AdditionalCaseInfoType) objectMapper(
            oldCaseData.getAdditionalCaseInfoType(), AdditionalCaseInfoType.class, mapper));
        caseData.setCorrespondenceScotType((CorrespondenceScotType) objectMapper(
            oldCaseData.getCorrespondenceScotType(), CorrespondenceScotType.class, mapper));
        caseData.setCorrespondenceType((CorrespondenceType) objectMapper(
            oldCaseData.getCorrespondenceType(), CorrespondenceType.class, mapper));
        caseData.setAddressLabelsSelectionType((AddressLabelsSelectionType) objectMapper(
            oldCaseData.getAddressLabelsSelectionType(), AddressLabelsSelectionType.class, mapper));
        caseData.setAddressLabelsAttributesType((AddressLabelsAttributesType) objectMapper(
            oldCaseData.getAddressLabelsAttributesType(), AddressLabelsAttributesType.class, mapper));
        caseData.setCaseNotes(oldCaseData.getCaseNotes());
        caseData.setClaimantWorkAddress((ClaimantWorkAddressType) objectMapper(
            oldCaseData.getClaimantWorkAddress(), ClaimantWorkAddressType.class, mapper));
        caseData.setClaimantRepresentedQuestion(oldCaseData.getClaimantRepresentedQuestion());
        caseData.setCaseSource(oldCaseData.getCaseSource());
        caseData.setConciliationTrack(oldCaseData.getConciliationTrack());
        caseData.setCounterClaim(oldCaseData.getCounterClaim());
        caseData.setRestrictedReporting((
            RestrictedReportingType) objectMapper(oldCaseData.getRestrictedReporting(),
                                                  RestrictedReportingType.class, mapper));
        caseData.setTargetHearingDate(oldCaseData.getTargetHearingDate());
        caseData.setClaimant(oldCaseData.getClaimant());
        caseData.setRespondent(oldCaseData.getRespondent());
        caseData.setEQP(oldCaseData.getEqp());
        caseData.setFlag1(oldCaseData.getFlag1());
        caseData.setFlag2(oldCaseData.getFlag2());
        caseData.setDocMarkUp(oldCaseData.getDocMarkUp());
        caseData.setCaseRefECC(oldCaseData.getCaseRefECC());
        caseData.setRespondentECC((DynamicFixedListType) objectMapper(
            oldCaseData.getRespondentECC(), DynamicFixedListType.class, mapper));
        caseData.setCcdID(oldCaseData.getCcdID());
        caseData.setFlagsImageFileName(oldCaseData.getFlagsImageFileName());
        caseData.setFlagsImageAltText(oldCaseData.getFlagsImageAltText());
        caseData.setCompanyPremises((CompanyPremisesType) objectMapper(
            oldCaseData.getCompanyPremises(), CompanyPremisesType.class, mapper));
        caseData.setReasonForCT(oldCaseData.getReasonForCT());
        if (state != null && !state.equals(CLOSED_STATE)) {
            caseData.setPositionType(POSITION_TYPE_CASE_TRANSFERRED_OTHER_COUNTRY);
        }
        caseData.setDocumentCollection(createDocumentCollection(oldCaseData.getDocumentCollection(), mapper));
        caseData.setJurCodesCollection(createJurCodesCollection(oldCaseData.getJurCodesCollection(), mapper));
        caseData.setAddressLabelCollection(createAddressLabelCollection(
            oldCaseData.getAddressLabelCollection(), mapper));
        if (oldCaseData.getFileLocation() != null) {
            caseData.setFileLocation(oldCaseData.getFileLocation().getSelectedCode());
        }

        if (oldCaseData.getClerkResponsible() != null) {
            caseData.setClerkResponsible(oldCaseData.getClerkResponsible().getSelectedCode());
        }
        if (caseTypeId.equals(SCOTLAND_CASE_TYPE_ID)) {
            copyScotlandData(oldCaseData, caseData);
        }
        caseData.setRespondentCollection(createRespondentCollection(oldCaseData.getRespondentCollection(), mapper));
        caseData.setLinkedCaseCT(generateMarkUp(ccdGatewayBaseUrl, caseId, oldCaseData.getEthosCaseReference()));
        return caseData;
    }

    private static ClaimantIndType mapClaimantIndType(uk.gov.hmcts.et.common.model.ccd.CaseData oldCaseData,
                                                      ObjectMapper mapper) {
        ClaimantIndType claimantIndType = (ClaimantIndType) objectMapper(oldCaseData.getClaimantIndType(),
                                                                 ClaimantIndType.class, mapper);
        if (isEmpty(oldCaseData.getClaimantIndType())) {
            return claimantIndType;
        }

        setClaimantGender(oldCaseData, claimantIndType);
        setClaimantTitle(oldCaseData, claimantIndType);
        return claimantIndType;
    }

    private static void setClaimantGender(uk.gov.hmcts.et.common.model.ccd.CaseData oldCaseData,
                                  ClaimantIndType claimantIndType) {
        switch (defaultIfEmpty(oldCaseData.getClaimantIndType().getClaimantSex(), "")) {
            case "Male" -> claimantIndType.setClaimantGender("Male");
            case "Female" -> claimantIndType.setClaimantGender("Female");
            default -> claimantIndType.setClaimantGender(null);
        }
    }

    private static void setClaimantTitle(uk.gov.hmcts.et.common.model.ccd.CaseData oldCaseData,
                                  ClaimantIndType claimantIndType) {
        switch (defaultIfEmpty(oldCaseData.getClaimantIndType().getClaimantPreferredTitle(), "")) {
            case "Mr" -> claimantIndType.setClaimantTitle("Mr");
            case "Mrs" -> claimantIndType.setClaimantTitle("Mrs");
            case "Miss" -> claimantIndType.setClaimantTitle("Miss");
            case "Ms" -> claimantIndType.setClaimantTitle("Ms");
            case "Mx" -> claimantIndType.setClaimantTitle("Mx");
            case "Other" -> {
                claimantIndType.setClaimantTitle("Other");
                claimantIndType.setClaimantTitleOther(oldCaseData.getClaimantIndType().getClaimantTitleOther());
            }
            default -> claimantIndType.setClaimantTitle(null);
        }
    }

    private static void copyScotlandData(uk.gov.hmcts.et.common.model.ccd.CaseData oldCaseData, CaseData caseData) {
        caseData.setManagingOffice(oldCaseData.getManagingOffice().equals(UNASSIGNED_OFFICE)
                                       ? TribunalOffice.GLASGOW.getOfficeName()
                                       : oldCaseData.getManagingOffice());
        caseData.setAllocatedOffice(oldCaseData.getAllocatedOffice().equals(UNASSIGNED_OFFICE)
                                        ? TribunalOffice.GLASGOW.getOfficeName()
                                        : oldCaseData.getAllocatedOffice());

        if (oldCaseData.getFileLocationAberdeen() != null) {
            caseData.setFileLocationAberdeen(oldCaseData.getFileLocationAberdeen().getSelectedCode());
        }
        if (oldCaseData.getFileLocationDundee() != null) {
            caseData.setFileLocationDundee(oldCaseData.getFileLocationDundee().getSelectedCode());
        }
        if (oldCaseData.getFileLocationEdinburgh() != null) {
            caseData.setFileLocationEdinburgh(oldCaseData.getFileLocationEdinburgh().getSelectedCode());
        }
        if (oldCaseData.getFileLocationGlasgow() != null) {
            caseData.setFileLocationGlasgow(oldCaseData.getFileLocationGlasgow().getSelectedCode());
        }
    }

    private static List<RespondentSumTypeItem> createRespondentCollection(
        List<uk.gov.hmcts.et.common.model.ccd.items.RespondentSumTypeItem> respondentCollection, ObjectMapper mapper) {
        List<RespondentSumTypeItem> respondentSumTypeItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(respondentCollection)) {
            for (uk.gov.hmcts.et.common.model.ccd.items.RespondentSumTypeItem respondent : respondentCollection) {
                RespondentSumType respondentSumType = (RespondentSumType) objectMapper(
                    respondent.getValue(), RespondentSumType.class, mapper);
                RespondentSumTypeItem respondentSumTypeItem = new RespondentSumTypeItem();
                respondentSumTypeItem.setId(UUID.randomUUID().toString());
                respondentSumTypeItem.setValue(respondentSumType);
                respondentSumTypeItems.add(respondentSumTypeItem);
            }
        }

        return respondentSumTypeItems;
    }

    private static List<AddressLabelTypeItem> createAddressLabelCollection(
        List<uk.gov.hmcts.et.common.model.ccd.items.AddressLabelTypeItem> addressLabelCollection,
        ObjectMapper mapper) {
        List<AddressLabelTypeItem> addressLabelTypeItemList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(addressLabelCollection)) {
            for (uk.gov.hmcts.et.common.model.ccd.items.AddressLabelTypeItem addressLabel : addressLabelCollection) {
                AddressLabelType addressLabelType = (AddressLabelType) objectMapper(
                    addressLabel.getValue(), AddressLabelType.class, mapper);
                AddressLabelTypeItem addressLabelTypeItem = new AddressLabelTypeItem();
                addressLabelTypeItem.setId(UUID.randomUUID().toString());
                addressLabelTypeItem.setValue(addressLabelType);
                addressLabelTypeItemList.add(addressLabelTypeItem);
            }
        }

        return addressLabelTypeItemList;
    }

    private static List<JurCodesTypeItem> createJurCodesCollection(
        List<uk.gov.hmcts.et.common.model.ccd.items.JurCodesTypeItem> jurCodesCollection, ObjectMapper mapper) {
        List<JurCodesTypeItem> jurCodesTypeItemList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jurCodesCollection)) {
            for (uk.gov.hmcts.et.common.model.ccd.items.JurCodesTypeItem jurCodes : jurCodesCollection) {
                JurCodesType jurCodesType = (JurCodesType)
                    objectMapper(jurCodes.getValue(), JurCodesType.class, mapper);
                JurCodesTypeItem  jurCodesTypeItem = new JurCodesTypeItem();
                jurCodesTypeItem.setId(UUID.randomUUID().toString());
                jurCodesTypeItem.setValue(jurCodesType);
                jurCodesTypeItemList.add(jurCodesTypeItem);
            }
        }

        return jurCodesTypeItemList;
    }

    private static List<DocumentTypeItem> createDocumentCollection(
        List<uk.gov.hmcts.et.common.model.ccd.items.DocumentTypeItem> documentCollection, ObjectMapper mapper) {
        List<DocumentTypeItem> documentTypeItemsList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(documentCollection)) {
            for (uk.gov.hmcts.et.common.model.ccd.items.DocumentTypeItem document : documentCollection) {
                DocumentType documentType = new DocumentType();
                documentType.setOwnerDocument(document.getValue().getOwnerDocument());
                documentType.setTypeOfDocument(document.getValue().getTypeOfDocument());
                documentType.setUploadedDocument((UploadedDocumentType) objectMapper(
                    document.getValue().getUploadedDocument(), UploadedDocumentType.class, mapper));
                documentType.setCreationDate(document.getValue().getCreationDate());
                documentType.setShortDescription(document.getValue().getShortDescription());
                DocumentTypeItem documentTypeItem = new DocumentTypeItem();
                documentTypeItem.setId(UUID.randomUUID().toString());
                documentTypeItem.setValue(documentType);
                documentTypeItemsList.add(documentTypeItem);
            }
        }
        return documentTypeItemsList;
    }

    public static String generateMarkUp(String ccdGatewayBaseUrl, String caseId, String ethosCaseRef) {
        var url = ccdGatewayBaseUrl + "/cases/case-details/" + caseId;
        return "<a target=\"_blank\" href=\"" + url + "\">" + ethosCaseRef + "</a>";
    }

    public static Object objectMapper(Object object, Class<?> classType, ObjectMapper mapper) {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper.convertValue(object, classType);
    }

}
