package wvly.jobs.api

import wvly.models.jobs.JobId
import wvly.models.jobs.JobStatus

interface JobStatusViewer {
    fun getJobStatus(jobId: JobId): JobStatus
}
