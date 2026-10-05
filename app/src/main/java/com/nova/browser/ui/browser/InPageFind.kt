package com.nova.browser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InPageFind(
    visible: Boolean,
    matchCount: Int,
    currentMatch: Int,
    onSearch: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(visible) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search, null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onNext() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )

            if (query.isNotBlank()) {
                Text(
                    text = if (matchCount > 0) "$currentMatch/$matchCount" else "0/0",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, "Next",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            IconButton(
                onClick = {
                    keyboard?.hide()
                    onDismiss()
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Close, "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}