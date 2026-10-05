package com.nova.browser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UrlPopup(
    visible: Boolean,
    url: String,
    maximized: Boolean,
    onUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onRefresh: () -> Unit,
    onToggleMaximize: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val statusBarPad = WindowInsets.statusBars.asPaddingValues()
    val topPad = statusBarPad.calculateTopPadding()

    // Local edit state — keyed to url so it resets when tab/url changes externally
    var fieldValue by remember(url) {
        mutableStateOf(
            TextFieldValue(
                text = url,
                selection = TextRange(0, url.length),
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Request focus when popup opens or maximize toggles
    LaunchedEffect(visible, maximized) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val selectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
    )

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            // ── Background scrim that closes popup on tap ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        keyboard?.hide()
                        onDismiss()
                    }
            )

            // ── Popup content (sits on top, doesn't inherit scrim click) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = topPad + 52.dp,
                        start = 8.dp,
                        end = 8.dp,
                    ),
            ) {
                if (!maximized) {
                    // ── Small popup row ────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = fieldValue,
                            onValueChange = {
                                fieldValue = it
                                onUrlChange(it.text)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = { onNavigate() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )
                        Spacer(Modifier.width(2.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh, "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onToggleMaximize,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Fullscreen, "Maximize",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = onNavigate,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Send, "Go",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    // ── Maximized rectangular editor ──────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Edit URL",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = onToggleMaximize,
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Default.FullscreenExit, "Minimize",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    keyboard?.hide()
                                    onDismiss()
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close, "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 240.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141414))
                                .padding(12.dp),
                        ) {
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = {
                                    fieldValue = it
                                    onUrlChange(it.text)
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go,
                                ),
                                keyboardActions = KeyboardActions(onGo = { onNavigate() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onRefresh() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.Refresh, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh", color = Color.White, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onNavigate() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.Send, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Go",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}