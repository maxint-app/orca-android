package com.maxint.orca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.maxint.orca.core.Orca
import com.maxint.orca.core.OrcaConfiguration
import com.maxint.orca.core.OrcaEnvironment
import com.maxint.orca.ui.paywall.PaywallScreen
import com.maxint.orca.ui.theme.OrcaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Orca.configure(
            context = this,
            config = OrcaConfiguration(
                publicKey = "YOUR_PUBLIC_KEY",
                environment = OrcaEnvironment.SANDBOX,
                customerEmail = null
            )
        )

        setContent {
            OrcaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showPaywall by remember { mutableStateOf(true) }

                    if (showPaywall) {
                        PaywallScreen(
                            customerEmail = "test@example.com",
                            onDismiss = { showPaywall = false }
                        )
                    }
                }
            }
        }
    }
}
