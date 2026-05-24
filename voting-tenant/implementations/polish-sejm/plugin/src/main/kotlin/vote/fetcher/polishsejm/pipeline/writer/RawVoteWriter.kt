@file:OptIn(ExperimentalStdlibApi::class)

package vote.fetcher.polishsejm.pipeline.writer

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import vote.fetcher.polishsejm.data.VotingDetails
import wvly.jobs.api.JobStatusService
import wvly.models.jobs.JobId
import wvly.models.jobs.JobStepId
import wvly.models.tenants.VotingTenant
import wvly.models.votes.RawVote
import wvly.storage.api.cache.RawVoteCache

class RawVoteWriter {
    fun writeAll(
        jobId: JobId,
        jobStatusService: JobStatusService,
        parentStepId: JobStepId?,
        allDayVotings: List<VotingDetails>,
        rawVoteCache: RawVoteCache,
    ) {
        var totalWritten = 0

        val moshiAdapter = Moshi.Builder().build().adapter<VotingDetails>()

        for (voting in allDayVotings) {
            val content = moshiAdapter.toJson(voting)
            rawVoteCache.put(
                handledTenant = VotingTenant("polish-sejm"),
                content = RawVote(content),
            )
            totalWritten++
        }

        jobStatusService.addStepToStep(
            stepId = parentStepId ?: createRoot(jobId, jobStatusService),
            name = "Wrote [$totalWritten] raw vote entries to cache",
            description = "Pipeline completed successfully.",
        )
    }

    private fun createRoot(
        jobId: JobId,
        jobStatusService: JobStatusService,
    ): JobStepId =
        jobStatusService.addStepToJob(
            jobId = jobId,
            name = "Raw vote writing (fallback root)",
            description = "Fallback root step created when parent is null.",
        )
}
