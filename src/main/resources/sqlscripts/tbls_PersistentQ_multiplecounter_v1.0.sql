/* CREATE TABLES FOR PERSISTENT QUEUE */

-- =============================================
-- Author:		Mohammed Hafejee
--
-- Create date: 10-JUN-2020
-- Description:	Script to create base table used by ECM multiples persistent queue
-- Called by  : fn_persistentQ_getNextMultipleCountVal
-- VERSION	  :	10-JUN-2020		1.0  - Initial
--            : 29-OCT-2021     2.0  - CCD Consolidation
-- =============================================

/***********   multipleCounter   ************/

DROP TABLE IF EXISTS multiple_counter;
CREATE TABLE multiple_counter
  (
  multipleRef  varchar(25),
  counter integer DEFAULT 1
  );

CREATE INDEX IX_multipleCounter_multipleRef ON multiple_counter(multipleRef);

