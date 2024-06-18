package uk.gov.hmcts.reform.ethos.ecm.consumer.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkItem {
    private Integer id;
    private String caseId;
    private String jsonData;
}
