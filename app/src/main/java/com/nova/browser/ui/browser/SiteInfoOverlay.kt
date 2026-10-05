package com.nova.browser.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.browser.data.ConnectionInfo
import com.nova.browser.data.CookieReader
import com.nova.browser.data.SiteInfo

@Composable
fun SiteInfoOverlay(
    url: String,
    trackersBlocked: Int,
    onDismiss: () -> Unit,
) {
    val info = SiteInfo.analyze(url)
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val ctx = LocalContext.current

    val secure = ConnectionInfo.isSecure(url)
    val insecure = ConnectionInfo.isInsecure(url)

    var cookies by remember(url) { mutableStateOf(CookieReader.cookiesFor(url)) }
    var showAllCookies by remember(url) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarPadding.calculateBottomPadding())
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { }
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        secure -> Icons.Default.Lock
                        insecure -> Icons.Default.Warning
                        else -> Icons.Default.Language
                    },
                    contentDescription = null,
                    tint = when {
                        secure -> Color(0xFF34D399)
                        insecure -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            secure -> "Connection is secure"
                            insecure -> "Not secure"
                            else -> "Internal page"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        info.host.ifBlank { "unknown" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Tracker card ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Lock, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "$trackersBlocked trackers & ads blocked",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "On this page",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── URL details ─────────────────────────────
            Section("URL Details")
            InfoRow("Scheme", info.scheme)
            InfoRow("Host", info.host)
            InfoRow("Port", info.port)
            InfoRow("Path", info.path)
            if (info.query.isNotBlank()) InfoRow("Query", info.query)
            InfoRow("TLD", if (info.tld.isNotBlank()) ".${info.tld}" else "—")

            Spacer(Modifier.height(16.dp))

            // ── Cookies ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Cookies",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${cookies.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (cookies.isEmpty()) {
                Text(
                    "No cookies for this site",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            } else {
                val visible = if (showAllCookies) cookies else cookies.take(5)

                visible.forEach { cookie ->
                    CookieRow(cookie)
                    Spacer(Modifier.height(6.dp))
                }

                if (cookies.size > 5 && !showAllCookies) {
                    Text(
                        text = "Show ${cookies.size - 5} more…",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAllCookies = true }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Copy buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CopyButton(
                        modifier = Modifier.weight(1f),
                        label = "Copy all",
                    ) {
                        val text = CookieReader.buildPretty(cookies)
                        copyToClipboard(ctx, "Cookies", text)
                    }
                    CopyButton(
                        modifier = Modifier.weight(1f),
                        label = "Copy header",
                    ) {
                        val text = CookieReader.buildCookieHeader(cookies)
                        copyToClipboard(ctx, "Cookie header", text)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Security ─────────────────────────────────
            Section("Security")
            CheckRow("Encrypted (HTTPS)", secure)
            CheckRow("No mixed content risk", secure)
            CheckRow("Known domain type", info.domainAgeHint.isNotBlank())

            Spacer(Modifier.height(16.dp))

            Section("Domain Type")
            Text(
                info.domainAgeHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CookieRow(cookie: CookieReader.CookieEntry) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = cookie.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (cookie.secure) {
                Text(
                    "secure",
                    color = Color(0xFF34D399),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (expanded) cookie.value else cookie.maskedValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = if (expanded) 4 else 1,
        )
    }
}

@Composable
private fun CopyButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.ContentCopy, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun CheckRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Check, null,
            tint = if (ok) Color(0xFF34D399) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value.ifBlank { "—" },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) { }
}