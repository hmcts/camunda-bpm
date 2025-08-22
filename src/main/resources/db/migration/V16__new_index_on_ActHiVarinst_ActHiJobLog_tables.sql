CREATE INDEX CONCURRENTLY IF NOT EXISTS act_idx_hi_job_log_job_def_config on public.act_hi_job_log USING btree (job_def_configuration_);

CREATE INDEX CONCURRENTLY IF NOT EXISTS act_idx_hi_varinst_name_vartype_text ON public.act_hi_varinst USING btree (NAME_, VAR_TYPE_, TEXT_);

