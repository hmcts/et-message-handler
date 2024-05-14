CREATE TABLE multiple_errors
(
    id           serial PRIMARY KEY,
    multipleRef  varchar(25),
    ethosCaseRef varchar(25),
    description  varchar(250)
);

CREATE INDEX IX_multipleErrors_multipleRef_ethosCaseRef ON multiple_errors(multipleRef,ethosCaseRef);