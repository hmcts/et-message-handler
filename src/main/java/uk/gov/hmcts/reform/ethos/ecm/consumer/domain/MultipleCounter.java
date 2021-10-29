package uk.gov.hmcts.reform.ethos.ecm.consumer.domain;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@NoArgsConstructor
@Table(name = "multiple_counter")
public class MultipleCounter {

    @Id
    protected String multipleref;
    protected Integer counter;
}
