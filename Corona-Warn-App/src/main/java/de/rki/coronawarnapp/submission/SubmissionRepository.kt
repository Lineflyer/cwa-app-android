package de.rki.coronawarnapp.submission

import androidx.annotation.VisibleForTesting
import de.rki.coronawarnapp.datadonation.analytics.modules.keysubmission.AnalyticsKeySubmissionCollector
import de.rki.coronawarnapp.datadonation.analytics.modules.registeredtest.TestResultDataCollector
import de.rki.coronawarnapp.deadman.DeadmanNotificationScheduler
import de.rki.coronawarnapp.exception.ExceptionCategory
import de.rki.coronawarnapp.exception.NoRegistrationTokenSetException
import de.rki.coronawarnapp.exception.http.CwaWebException
import de.rki.coronawarnapp.exception.reporting.report
import de.rki.coronawarnapp.playbook.BackgroundNoise
import de.rki.coronawarnapp.service.submission.SubmissionService
import de.rki.coronawarnapp.storage.TracingSettings
import de.rki.coronawarnapp.submission.data.tekhistory.TEKHistoryStorage
import de.rki.coronawarnapp.util.DeviceUIState
import de.rki.coronawarnapp.util.NetworkRequestWrapper
import de.rki.coronawarnapp.util.NetworkRequestWrapper.Companion.withSuccess
import de.rki.coronawarnapp.util.TimeStamper
import de.rki.coronawarnapp.util.coroutine.AppScope
import de.rki.coronawarnapp.util.formatter.TestResult
import de.rki.coronawarnapp.worker.BackgroundWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class SubmissionRepository @Inject constructor(
    private val submissionSettings: SubmissionSettings,
    private val submissionService: SubmissionService,
    @AppScope private val scope: CoroutineScope,
    private val timeStamper: TimeStamper,
    private val tekHistoryStorage: TEKHistoryStorage,
    private val deadmanNotificationScheduler: DeadmanNotificationScheduler,
    private val backgroundNoise: BackgroundNoise,
    private val analyticsKeySubmissionCollector: AnalyticsKeySubmissionCollector,
    private val tracingSettings: TracingSettings,
    private val testResultDataCollector: TestResultDataCollector
) {
    private val testResultReceivedDateFlowInternal =
        MutableStateFlow((submissionSettings.initialTestResultReceivedAt ?: timeStamper.nowUTC).toDate())
    val testResultReceivedDateFlow: Flow<Date> = testResultReceivedDateFlowInternal

    private val deviceUIStateFlowInternal =
        MutableStateFlow<NetworkRequestWrapper<DeviceUIState, Throwable>>(NetworkRequestWrapper.RequestIdle)
    val deviceUIStateFlow: Flow<NetworkRequestWrapper<DeviceUIState, Throwable>> = deviceUIStateFlowInternal

    // to be used by new submission flow screens
    val hasGivenConsentToSubmission = submissionSettings.hasGivenConsent.flow
    val hasViewedTestResult = submissionSettings.hasViewedTestResult.flow
    val currentSymptoms = submissionSettings.symptoms

    private val testResultFlow = MutableStateFlow<TestResult?>(null)

    // to be used by new submission flow screens
    fun giveConsentToSubmission() {
        Timber.d("giveConsentToSubmission()")
        submissionSettings.hasGivenConsent.update {
            true
        }
    }

    // to be used by new submission flow screens
    fun revokeConsentToSubmission() {
        Timber.d("revokeConsentToSubmission()")
        submissionSettings.hasGivenConsent.update {
            false
        }
    }

    // to be set to true once the user has opened and viewed their test result
    fun setViewedTestResult() {
        Timber.d("setViewedTestResult()")
        submissionSettings.hasViewedTestResult.update {
            true
        }
    }

    fun refreshDeviceUIState(refreshTestResult: Boolean = true) {
        if (submissionSettings.isSubmissionSuccessful) {
            deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestSuccessful(DeviceUIState.SUBMITTED_FINAL)
            return
        }

        val registrationToken = submissionSettings.registrationToken.value
        if (registrationToken == null) {
            deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestSuccessful(DeviceUIState.UNPAIRED)
            return
        }

        if (submissionSettings.isAllowedToSubmitKeys) {
            deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestSuccessful(DeviceUIState.PAIRED_POSITIVE)
            return
        }

        var refresh = refreshTestResult
        deviceUIStateFlowInternal.value.withSuccess {
            if (it != DeviceUIState.PAIRED_NO_RESULT && it != DeviceUIState.UNPAIRED) {
                refresh = false
                Timber.d("refreshDeviceUIState: Change refresh, state ${it.name} doesn't require refresh")
            }
        }

        if (refresh) {
            deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestStarted

            scope.launch {
                try {
                    deviceUIStateFlowInternal.value =
                        NetworkRequestWrapper.RequestSuccessful(fetchTestResult(registrationToken))
                } catch (err: CwaWebException) {
                    deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestFailed(err)
                } catch (err: Exception) {
                    deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestFailed(err)
                    err.report(ExceptionCategory.INTERNAL)
                }
            }
        } else {
            deviceUIStateFlowInternal.value =
                NetworkRequestWrapper.RequestSuccessful(deriveUiState(testResultFlow.value))
        }
    }

    suspend fun asyncRegisterDeviceViaTAN(tan: String) {
        analyticsKeySubmissionCollector.reset()
        val registrationData = submissionService.asyncRegisterDeviceViaTAN(tan)
        submissionSettings.registrationToken.update {
            registrationData.registrationToken
        }
        updateTestResult(registrationData.testResult)
        submissionSettings.devicePairingSuccessfulAt = timeStamper.nowUTC
        backgroundNoise.scheduleDummyPattern()
        analyticsKeySubmissionCollector.reportTestRegistered()
        analyticsKeySubmissionCollector.reportRegisteredWithTeleTAN()
    }

    suspend fun asyncRegisterDeviceViaGUID(guid: String): TestResult {
        analyticsKeySubmissionCollector.reset()
        val registrationData = submissionService.asyncRegisterDeviceViaGUID(guid)
        submissionSettings.registrationToken.update {
            registrationData.registrationToken
        }
        updateTestResult(registrationData.testResult) // This saves initial time
        submissionSettings.devicePairingSuccessfulAt = timeStamper.nowUTC
        backgroundNoise.scheduleDummyPattern()
        analyticsKeySubmissionCollector.reportTestRegistered()
        testResultDataCollector.saveTestResultAnalyticsSettings(registrationData.testResult) // This saves received at
        return registrationData.testResult
    }

    suspend fun reset() {
        deviceUIStateFlowInternal.value = NetworkRequestWrapper.RequestIdle
        tekHistoryStorage.clear()
        submissionSettings.clear()
    }

    @VisibleForTesting
    fun updateTestResult(testResult: TestResult) {
        testResultFlow.value = testResult

        testResultDataCollector.updatePendingTestResultReceivedTime(testResult)

        if (testResult == TestResult.POSITIVE) {
            submissionSettings.isAllowedToSubmitKeys = true
            analyticsKeySubmissionCollector.reportPositiveTestResultReceived()
            deadmanNotificationScheduler.cancelScheduledWork()
        }

        // https://jira-ibs.wbs.net.sap/browse/EXPOSUREAPP-4484
        // User removed a test before 1.11 where due to a bug the timestamp was not removed.
        if (submissionSettings.initialTestResultReceivedAt != null &&
            submissionSettings.registrationToken.value != null &&
            submissionSettings.devicePairingSuccessfulAt == null
        ) {
            Timber.tag(TAG).w("User has stale initialTestResultReceivedAt, fixing EXPOSUREAPP-4484.")
            submissionSettings.initialTestResultReceivedAt = null
        }

        val initialTestResultReceivedTimestamp = submissionSettings.initialTestResultReceivedAt

        if (initialTestResultReceivedTimestamp == null) {
            val currentTime = timeStamper.nowUTC
            submissionSettings.initialTestResultReceivedAt = currentTime
            testResultReceivedDateFlowInternal.value = currentTime.toDate()
            if (testResult == TestResult.PENDING) {
                BackgroundWorkScheduler.startWorkScheduler()
            }
        } else {
            testResultReceivedDateFlowInternal.value = initialTestResultReceivedTimestamp.toDate()
        }
    }

    private suspend fun fetchTestResult(registrationToken: String): DeviceUIState = try {
        val testResult = submissionService.asyncRequestTestResult(registrationToken)
        updateTestResult(testResult)
        deriveUiState(testResult)
    } catch (err: NoRegistrationTokenSetException) {
        DeviceUIState.UNPAIRED
    }

    fun removeTestFromDevice() {
        submissionSettings.hasViewedTestResult.update { false }
        submissionSettings.hasGivenConsent.update { false }
        revokeConsentToSubmission()
        submissionSettings.registrationToken.update { null }
        submissionSettings.devicePairingSuccessfulAt = null
        tracingSettings.initialPollingForTestResultTimeStamp = 0L
        submissionSettings.initialTestResultReceivedAt = null
        submissionSettings.isAllowedToSubmitKeys = false
        tracingSettings.isTestResultAvailableNotificationSent = false
        submissionSettings.isSubmissionSuccessful = false
        testResultDataCollector.clear()
    }

    private fun deriveUiState(testResult: TestResult?): DeviceUIState = when (testResult) {
        TestResult.NEGATIVE -> DeviceUIState.PAIRED_NEGATIVE
        TestResult.POSITIVE -> DeviceUIState.PAIRED_POSITIVE
        TestResult.PENDING -> DeviceUIState.PAIRED_NO_RESULT
        TestResult.REDEEMED -> DeviceUIState.PAIRED_REDEEMED
        TestResult.INVALID -> DeviceUIState.PAIRED_ERROR
        null -> DeviceUIState.UNPAIRED
    }

    companion object {
        private const val TAG = "SubmissionRepository"
    }
}
