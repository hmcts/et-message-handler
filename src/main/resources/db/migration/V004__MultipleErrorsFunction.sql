CREATE OR REPLACE FUNCTION fn_persistentQ_logMultipleError (
    p_multipleRef varchar(25), p_ethosCaseRef varchar(25), p_description varchar(250))
RETURNS varchar(5) AS $$
-- =============================================
-- Author:		Mohammed Hafejee

-- TEST :		SELECT fn_persistentQ_logMultipleError  ('3265','2400001/2020','Unprocessable State');
-- TEST :		SELECT fn_persistentQ_logMultipleError  ('3265','2400001/2020','Single Case Taken');
--
-- Create date: 10-JUN-2020
-- Description:	Function to log error messages encountered during the operation of the ECM multiples persistent queue
-- VERSION	  :	10-JUN-2020		1.0  - Initial
--            :	16-JUN-2020		1.1  - Now returns arbitrary value 'ok' to prevent JPA error caused by void return
--            :	18-JUN-2020		1.2  - Added drop function before creation to prevent error when running script against existing function
--                                    with void return type
--            :	19-APR-2021		1.3  - Added explicit column identifiers in INSERT to cater for identity column now added
--            : 29-OCT-2021     2.0  - CCD Consolidation
-- =============================================
BEGIN
    INSERT INTO multiple_errors (multipleRef,ethosCaseRef,description)
    VALUES (p_multipleRef, p_ethosCaseRef, p_description);

    RETURN 'ok';
END;
$$ LANGUAGE plpgsql;
