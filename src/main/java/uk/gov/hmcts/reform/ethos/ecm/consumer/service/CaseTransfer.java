package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.et.common.model.ccd.CaseData;

@Builder
@Getter
public class CaseTransfer {
    private long caseId;
    private boolean transferSameCountry;
    private String caseTypeId;
    private String jurisdiction;
    private CaseData caseData;
    private String officeCT;
    private String positionTypeCT;
    private String reasonCT;
    private String sourceEthosCaseReference;
}
