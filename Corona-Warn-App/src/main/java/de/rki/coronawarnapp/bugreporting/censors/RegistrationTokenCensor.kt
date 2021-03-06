package de.rki.coronawarnapp.bugreporting.censors

import dagger.Reusable
import de.rki.coronawarnapp.bugreporting.censors.BugCensor.Companion.toNewLogLineIfDifferent
import de.rki.coronawarnapp.bugreporting.debuglog.LogLine
import de.rki.coronawarnapp.submission.SubmissionSettings
import de.rki.coronawarnapp.util.CWADebug
import javax.inject.Inject

@Reusable
class RegistrationTokenCensor @Inject constructor(
    private val submissionSettings: SubmissionSettings
) : BugCensor {
    override suspend fun checkLog(entry: LogLine): LogLine? {
        val token = submissionSettings.registrationToken.value ?: return null
        if (!entry.message.contains(token)) return null

        val newMessage = if (CWADebug.isDeviceForTestersBuild) {
            entry.message.replace(token, PLACEHOLDER_TESTER + token.takeLast(27))
        } else {
            entry.message.replace(token, PLACEHOLDER + token.takeLast(4))
        }

        return entry.toNewLogLineIfDifferent(newMessage)
    }

    companion object {
        private const val PLACEHOLDER_TESTER = "########-"
        private const val PLACEHOLDER = "########-####-####-####-########"
    }
}
