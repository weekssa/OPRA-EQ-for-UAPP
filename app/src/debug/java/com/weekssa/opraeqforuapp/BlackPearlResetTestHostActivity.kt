package com.weekssa.opraeqforuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.weekssa.opraeqforuapp.domain.blackpearl.BlackPearlDeviceQualificationSnapshot
import com.weekssa.opraeqforuapp.domain.dac.HardwareEqSnapshotState
import com.weekssa.opraeqforuapp.domain.settings.ThemeMode
import com.weekssa.opraeqforuapp.ui.BlackPearlQualificationUiState
import com.weekssa.opraeqforuapp.ui.screens.BlackPearlDeviceResetSection
import com.weekssa.opraeqforuapp.ui.theme.OpraEqTheme
import kotlinx.coroutines.awaitCancellation

class BlackPearlResetTestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_CLEAR_JOURNAL, false)) {
            resetJournal().edit().clear().commit()
        }
        super.onCreate(savedInstanceState)

        setContent {
            val journal = remember { resetJournal() }
            val eqResetRequests = remember {
                mutableIntStateOf(journal.getInt(KEY_EQ_RESET_REQUESTS, 0))
            }
            val deviceWriteRequests = remember {
                mutableIntStateOf(journal.getInt(KEY_DEVICE_WRITE_REQUESTS, 0))
            }
            val initialState = remember {
                BlackPearlQualificationUiState().success(
                    BlackPearlDeviceQualificationSnapshot(
                        sessionGeneration = 7L,
                        firmwareVersion = "BP-TEST",
                        filterCode = 2,
                        gainModeCode = 1,
                        ampTopologyCode = 1,
                        micGainDb = 0,
                        leftBalanceDb = 0,
                        rightBalanceDb = 0,
                        playbackGainRaw = -1024,
                    ),
                )
            }

            OpraEqTheme(ThemeMode.Light) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text("TEST ONLY: all Black Pearl operations below are fake. No USB device is used.")
                    Text(
                        "Fake callback counts: EQ reset ${eqResetRequests.intValue}; " +
                            "DEVICE writes ${deviceWriteRequests.intValue}",
                    )
                    BlackPearlDeviceResetSection(
                        state = initialState,
                        hardwareEqState = HardwareEqSnapshotState(),
                        enabled = true,
                        onSetDeviceControl = { _, _ ->
                            record(KEY_DEVICE_WRITE_REQUESTS, deviceWriteRequests.intValue + 1)
                            deviceWriteRequests.intValue += 1
                        },
                        onResetEqToFlat = {
                            record(KEY_EQ_RESET_REQUESTS, eqResetRequests.intValue + 1)
                            eqResetRequests.intValue += 1
                            awaitCancellation()
                        },
                        onReadCurrentEq = {},
                        onRefreshDevice = {},
                        onMessage = {},
                        onOperationStatus = { _, _ -> },
                    )
                }
            }
        }
    }

    private fun resetJournal() = getSharedPreferences(JOURNAL_NAME, MODE_PRIVATE)

    private fun record(key: String, value: Int) {
        check(resetJournal().edit().putInt(key, value).commit())
    }

    private companion object {
        const val EXTRA_CLEAR_JOURNAL = "clear_reset_test_journal"
        const val JOURNAL_NAME = "black_pearl_reset_test_journal"
        const val KEY_EQ_RESET_REQUESTS = "eq_reset_requests"
        const val KEY_DEVICE_WRITE_REQUESTS = "device_write_requests"
    }
}
