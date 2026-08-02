package com.ermukhamed.deepresearch.cli

import com.ermukhamed.deepresearch.llm.LlmFailure

/**
 * Process exit codes.
 *
 * Distinct codes per failure class let a shell script or CI job react to *why* a
 * run failed — retrying a rate limit, but not a bad API key.
 */
enum class ExitCode(val code: Int) {
    SUCCESS(0),
    USAGE(2),
    CONFIGURATION(3),
    AUTHENTICATION(4),
    RATE_LIMITED(5),
    NETWORK(6),
    API(7),
    IO(8),
    ;

    companion object {
        /** Maps a completion failure onto the code the process should exit with. */
        fun forFailure(failure: LlmFailure): ExitCode = when (failure) {
            is LlmFailure.Authentication -> AUTHENTICATION
            is LlmFailure.RateLimited -> RATE_LIMITED
            is LlmFailure.Network -> NETWORK
            is LlmFailure.Api, is LlmFailure.MalformedResponse, LlmFailure.EmptyResponse -> API
        }
    }
}
