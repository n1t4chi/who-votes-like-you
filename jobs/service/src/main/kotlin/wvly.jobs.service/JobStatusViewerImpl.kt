package wvly.jobs.service

import wvly.jobs.api.JobStatusViewer
import wvly.models.jobs.*

class JobStatusViewerImpl(private val jobStatusStorage: JobStatusInMemoryStorage) : JobStatusViewer {
    override fun getJobStatus(jobId: JobId): JobStatus = jobStatusStorage.get(jobId)
}
