--
-- Added new index on act_hi_varinst table to improve performance. See ticket below for details
--
--     https://tools.hmcts.net/jira/browse/DTSPO-14453
--
--

CREATE INDEX CONCURRENTLY IF NOT EXISTS act_idx_hi_varinst_name_taskid on act_hi_varinst(task_id_,name_);