/* CREATE TABLES FOR PERSISTENT QUEUE */

-- =============================================
-- Author:		Mohammed Hafejee
--
-- Create date: 10-JUN-2020
-- Description:	Script to create base table used by ECM multiples persistent queue for error logging
-- Called by  : fn_persistentQ_logMultipleError
-- VERSION	  :	10-JUN-2020		1.0  - Initial
--        	  :	19-APR-2021		1.1  - Added identity column primary key
--            : 29-OCT-2021     2.0  - CCD Consolidation
-- =============================================

/***********   multipleErrors   ************/

DROP TABLE IF EXISTS multiple_errors;
CREATE TABLE multiple_errors
  (
  id           serial PRIMARY KEY,
  multipleRef  varchar(25),
  ethosCaseRef varchar(25),
  description varchar(250)
  );

CREATE INDEX IX_multipleErrors_multipleRef_ethosCaseRef ON multiple_errors(multipleRef,ethosCaseRef);

