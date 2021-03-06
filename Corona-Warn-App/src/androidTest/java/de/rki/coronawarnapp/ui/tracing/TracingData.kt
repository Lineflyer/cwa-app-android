package de.rki.coronawarnapp.ui.tracing

import de.rki.coronawarnapp.risk.RiskState
import de.rki.coronawarnapp.tracing.GeneralTracingStatus
import de.rki.coronawarnapp.tracing.states.IncreasedRisk
import de.rki.coronawarnapp.tracing.states.LowRisk
import de.rki.coronawarnapp.tracing.states.TracingDisabled
import de.rki.coronawarnapp.tracing.states.TracingFailed
import de.rki.coronawarnapp.tracing.ui.details.TracingDetailsState
import de.rki.coronawarnapp.tracing.ui.details.items.behavior.BehaviorIncreasedRiskBox
import de.rki.coronawarnapp.tracing.ui.details.items.behavior.BehaviorNormalRiskBox
import de.rki.coronawarnapp.tracing.ui.details.items.periodlogged.PeriodLoggedBox
import de.rki.coronawarnapp.tracing.ui.details.items.risk.IncreasedRiskBox
import de.rki.coronawarnapp.tracing.ui.details.items.risk.LowRiskBox
import de.rki.coronawarnapp.tracing.ui.details.items.risk.TracingDisabledBox
import de.rki.coronawarnapp.tracing.ui.details.items.risk.TracingFailedBox
import de.rki.coronawarnapp.tracing.ui.details.items.riskdetails.DetailsFailedCalculationBox
import de.rki.coronawarnapp.tracing.ui.details.items.riskdetails.DetailsIncreasedRiskBox
import de.rki.coronawarnapp.tracing.ui.details.items.riskdetails.DetailsLowRiskBox
import org.joda.time.Instant

object TracingData {

    val TRACING_DISABLED = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_INACTIVE,
            riskState = RiskState.LOW_RISK,
            isManualKeyRetrievalEnabled = false
        ),
        listOf(
            TracingDisabledBox.Item(
                state = TracingDisabled(
                    riskState = RiskState.LOW_RISK,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = Instant.now()
                )
            ),
            BehaviorNormalRiskBox.Item(
                tracingStatus = GeneralTracingStatus.Status.TRACING_INACTIVE,
                riskState = RiskState.LOW_RISK
            ),

            PeriodLoggedBox.Item(
                daysSinceInstallation = 4,
                tracingStatus = GeneralTracingStatus.Status.TRACING_INACTIVE
            ),
            DetailsLowRiskBox.Item(riskState = RiskState.LOW_RISK, matchedKeyCount = 0)
        )
    )

    val LOW_RISK = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
            riskState = RiskState.LOW_RISK,
            isManualKeyRetrievalEnabled = false
        ),
        listOf(
            LowRiskBox.Item(
                state = LowRisk(
                    riskState = RiskState.LOW_RISK,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = Instant.now(),
                    allowManualUpdate = false,
                    daysWithEncounters = 0,
                    daysSinceInstallation = 4,
                    lastEncounterAt = Instant.now()
                )
            ),
            BehaviorNormalRiskBox.Item(
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
                riskState = RiskState.LOW_RISK
            ),

            PeriodLoggedBox.Item(
                daysSinceInstallation = 4,
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE
            ),
            DetailsLowRiskBox.Item(riskState = RiskState.LOW_RISK, matchedKeyCount = 0)
        )
    )

    val LOW_RISK_WITH_ONE_ENCOUNTER = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
            riskState = RiskState.LOW_RISK,
            isManualKeyRetrievalEnabled = false
        ),
        listOf(
            LowRiskBox.Item(
                state = LowRisk(
                    riskState = RiskState.LOW_RISK,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = Instant.now(),
                    allowManualUpdate = false,
                    daysWithEncounters = 1,
                    daysSinceInstallation = 4,
                    lastEncounterAt = Instant.now()
                )
            ),
            BehaviorNormalRiskBox.Item(
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
                riskState = RiskState.LOW_RISK
            ),

            PeriodLoggedBox.Item(
                daysSinceInstallation = 4,
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE
            ),
            DetailsLowRiskBox.Item(riskState = RiskState.LOW_RISK, matchedKeyCount = 0)
        )
    )

    val LOW_RISK_WITH_TWO_ENCOUNTERS = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
            riskState = RiskState.LOW_RISK,
            isManualKeyRetrievalEnabled = false
        ),
        listOf(
            LowRiskBox.Item(
                state = LowRisk(
                    riskState = RiskState.LOW_RISK,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = Instant.now(),
                    allowManualUpdate = false,
                    daysWithEncounters = 2,
                    daysSinceInstallation = 4,
                    lastEncounterAt = Instant.now()
                )
            ),
            BehaviorNormalRiskBox.Item(
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
                riskState = RiskState.LOW_RISK
            ),

            PeriodLoggedBox.Item(
                daysSinceInstallation = 4,
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE
            ),
            DetailsLowRiskBox.Item(riskState = RiskState.LOW_RISK, matchedKeyCount = 0)
        )
    )

    val INCREASED_RISK = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
            riskState = RiskState.INCREASED_RISK,
            isManualKeyRetrievalEnabled = false
        ),
        listOf(
            IncreasedRiskBox.Item(
                state = IncreasedRisk(
                    riskState = RiskState.INCREASED_RISK,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = Instant.now(),
                    allowManualUpdate = false,
                    daysWithEncounters = 1,
                    lastEncounterAt = Instant.now()
                )
            ),
            BehaviorIncreasedRiskBox.Item,
            PeriodLoggedBox.Item(
                daysSinceInstallation = 5,
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE
            ),
            DetailsIncreasedRiskBox.Item(
                riskState = RiskState.INCREASED_RISK,
                lastEncounteredAt = Instant.now()
            )
        )
    )

    val TRACING_FAILED = Pair(
        TracingDetailsState(
            tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
            riskState = RiskState.CALCULATION_FAILED,
            isManualKeyRetrievalEnabled = true
        ),
        listOf(
            TracingFailedBox.Item(
                state = TracingFailed(
                    riskState = RiskState.CALCULATION_FAILED,
                    isInDetailsMode = true,
                    lastExposureDetectionTime = null
                )
            ),
            BehaviorNormalRiskBox.Item(
                tracingStatus = GeneralTracingStatus.Status.TRACING_ACTIVE,
                riskState = RiskState.CALCULATION_FAILED
            ),
            DetailsFailedCalculationBox.Item
        )
    )
}
