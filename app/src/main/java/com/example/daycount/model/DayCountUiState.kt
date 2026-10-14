package com.example.daycount.model

import java.time.LocalDate

data class DayCountUiState(
    val firstDate: LocalDate? = null,
    val secondDate: LocalDate? = null,
    val result: DayCountResult? = null,
    val history: List<DayCountHistory> = emptyList(),
    val errorMessage: String? = null,
    val isCalculating: Boolean = false,
    val calculationState: CalculationState = CalculationState.Idle,
    /** True only when Preserve Date Order is on and the end date precedes the start date. */
    val isEndBeforeStart: Boolean = false,
    /** True once the current range has been saved to history via COUNT DAYS. */
    val isSavedToHistory: Boolean = false,
    /** Incremented on every explicit COUNT DAYS so the result reveal can replay. */
    val countRequestId: Long = 0,
) {
    val canCount: Boolean get() = firstDate != null && secondDate != null
}

sealed interface CalculationState {
    data object Idle : CalculationState
    data object Calculated : CalculationState
    data class Error(val message: String) : CalculationState
}
