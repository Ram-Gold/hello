package com.example.hello.ui.quickaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.repository.ContactRepository
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.quickaction.components.FlowAAddContactCard
import com.example.hello.ui.quickaction.components.LocationExplainerDialog
import com.example.hello.ui.theme.HelloTheme

/**
 * Dedicated ultra-fast (<50ms cold start) Quick Add activity launched directly from home screen / Glance widget.
 * Styled with a translucent dialog theme and windowSoftInputMode=stateVisible for instant keyboard presentation.
 */
class QuickCaptureActivity : ComponentActivity() {

    private lateinit var viewModel: QuickActionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = HelloDatabase.getInstance(applicationContext)
        val alarmScheduler = FollowUpAlarmScheduler(applicationContext)
        val repo = ContactRepository(database = db, alarmScheduler = alarmScheduler)
        val locationService = LocationService(applicationContext)
        viewModel = QuickActionViewModel(repository = repo, locationService = locationService)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)

        setContent {
            HelloTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Intercept back button to step backward or dismiss immediately
                BackHandler {
                    if (uiState.flowAStep != FlowAStep.NAME) {
                        viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
                    } else {
                        finish()
                    }
                }

                // In-App Location Explainer Dialog if triggered
                if (uiState.showLocationExplainer) {
                    LocationExplainerDialog(
                        onAllowClick = {
                            viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))
                        },
                        onDismissClick = {
                            viewModel.onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Translucent scrim backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.36f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                finish()
                            }
                    )

                    // Single card resting directly on top of IME
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* Consume taps on card surface */ }
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        FlowAAddContactCard(
                            uiState = uiState,
                            onEvent = { event ->
                                when (event) {
                                    is QuickActionUiEvent.OnDismiss -> {
                                        finish()
                                    }
                                    is QuickActionUiEvent.OnQuickSaveFlowA -> {
                                        viewModel.onEvent(event)
                                        finish()
                                    }
                                    else -> {
                                        viewModel.onEvent(event)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
