package dev.crosspay.example

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
import dev.crosspay.example.ui.paywall.PaywallScreen
import dev.crosspay.example.ui.theme.OrcaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Orca.configure(
            context = this,
            config = OrcaConfiguration(
                publicKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjcm9zc3BheSIsInN1YiI6IjU0OTUwNGMwLTZlMjQtNDE2NS1hMjgxLWQyMTE4NGE0ZGNmZSIsImF1ZCI6WyJwdWJsaWMiLCJhMzhlYmMzYy0xZjFjLTRhMTgtOTFiNy1iYzZlMDIyMGZkMWMiXX0.ojYDgb25aRD68c0WgGS08XDXtrfBGM8sPaDacdZF69I",
                environment = OrcaEnvironment.SANDBOX,
                customerEmail = null,
                baseUrl = "http://localhost:8081"
            )
        )

        setContent {
            OrcaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showPaywall by remember { mutableStateOf(true) }

                    if (showPaywall) {
                        PaywallScreen(
                            customerEmail = "krtirtho@maxint.com",
                            onDismiss = { showPaywall = false }
                        )
                    }
                }
            }
        }
    }
}
