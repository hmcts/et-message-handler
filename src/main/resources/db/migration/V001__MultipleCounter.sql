CREATE TABLE multiple_counter
(
    multipleRef varchar(25),
    counter     integer DEFAULT 1
);

CREATE INDEX IX_multipleCounter_multipleRef ON multiple_counter(multipleRef);