package com.masar.portal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.masar.portal.R
import com.masar.portal.network.MasarApi
import com.masar.portal.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onConfigured: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_splash),
                contentDescription = null,
                modifier = Modifier.size(110.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "أهلاً بك في تطبيق مسار",
                style = MaterialTheme.typography.headlineMedium,
                color = TxtPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "أدخل رابط موقع شركتك للبدء",
                style = MaterialTheme.typography.bodyMedium,
                color = TxtDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                label = { Text("الرابط (مثال: vr2.umniate.com)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = Red, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val url = input.trim()
                        .removeSuffix("/")
                        .removePrefix("http://").removePrefix("https://")
                        .removePrefix("www.")
                    if (url.isEmpty() || !url.contains(".")) {
                        error = "الرابط غير صحيح"; return@Button
                    }
                    val full = "https://$url"
                    loading = true; error = null
                    scope.launch {
                        val ok = MasarApi(full).ping()
                        loading = false
                        if (ok) onConfigured(full) else error = "تعذّر الاتصال بالموقع"
                    }
                },
                enabled = !loading && input.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("متابعة", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
