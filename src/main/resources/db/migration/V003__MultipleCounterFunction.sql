﻿/* CREATE FUNCTION */

CREATE OR REPLACE FUNCTION fn_persistentQ_getNextMultipleCountVal (p_multipleRef varchar(25)) RETURNS Integer AS $$

-- =============================================
-- Author:		Mohammed Hafejee

-- TEST :		SELECT fn_persistentQ_getNextMultipleCountVal ('3265');
--
-- Create date: 10-JUN-2020
-- Description:	Function to return next incremental value for a multiple reference number passed in
-- VERSION	  :	10-JUN-2020	- 1.0  - Initial
--            : 29-OCT-2021 - 2.0  - CCD Consolidation
-- =============================================

    DECLARE currentval integer;

BEGIN
    -- Acquire Lock on multipleCounter table
    SELECT counter INTO currentval FROM multiple_counter WHERE multipleRef = p_multipleRef FOR UPDATE ;

    CASE
    WHEN currentval IS NULL THEN
        INSERT INTO multiple_counter(multipleRef) VALUES (p_multipleRef);
        currentval := 1;
    ELSE
        currentval = currentval + 1;
        UPDATE  multiple_counter SET counter = currentval WHERE multipleRef = p_multipleRef;
    END CASE;

    RETURN  currentval;
END;
$$ LANGUAGE plpgsql;


