package uk.gov.hmcts.reform.ethos.ecm.consumer.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "multiple_errors")
public class MultipleErrors {

    @Id
    private Long id;
    protected String multipleref;
    protected String ethoscaseref;
    protected String description;

    @Override
    public String toString() {
        return "Ethos Case Reference: '" + this.ethoscaseref
            + "', Description: '" + this.description + "'";
    }

}
