package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppCategory
import com.example.model.InstalledApp
import com.example.ui.components.DrawableAppIcon
import com.example.ui.theme.CockpitHairline
import com.example.ui.theme.CockpitObsidian
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceBright
import com.example.ui.theme.CockpitSurfaceElevated
import com.example.ui.theme.HavalAmber
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppDrawerScreen(
    apps: List<InstalledApp>,
    searchQuery: String,
    selectedCategory: AppCategory,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (AppCategory) -> Unit,
    onAppClick: (InstalledApp) -> Unit,
    onTogglePin: (InstalledApp) -> Unit,
    onOpenAppInfo: (InstalledApp) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_drawer_screen"),
        shape = RoundedCornerShape(24.dp),
        color = CockpitSurface.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: Back + Search + Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("drawer_back_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CockpitSurfaceElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад на главный экран"
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("drawer_search_input"),
                    placeholder = {
                        Text(
                            text = "Поиск приложений…",
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = HavalAmber
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Очистить поиск",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HavalAmber,
                        unfocusedBorderColor = CockpitHairline,
                        focusedContainerColor = CockpitSurfaceElevated,
                        unfocusedContainerColor = CockpitSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Text(
                    text = "${apps.size} прил.",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }

            // Category chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppCategory.entries) { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(category) },
                        label = {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("category_chip_${category.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HavalAmber,
                            selectedLabelColor = CockpitObsidian,
                            containerColor = CockpitSurfaceElevated,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = CockpitHairline
                        )
                    )
                }
            }

            // Grid of apps
            if (apps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Приложения не найдены",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Измените поисковый запрос или выберите другую категорию",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 190.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        AppDrawerItemCard(
                            app = app,
                            onClick = { onAppClick(app) },
                            onTogglePin = { onTogglePin(app) },
                            onOpenInfo = { onOpenAppInfo(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerItemCard(
    app: InstalledApp,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenInfo: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("drawer_app_${app.packageName}"),
        shape = RoundedCornerShape(18.dp),
        color = CockpitSurfaceElevated.copy(alpha = 0.92f),
        border = BorderStroke(
            width = 1.dp,
            color = if (app.isPinned) HavalAmber.copy(alpha = 0.6f) else CockpitHairline
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DrawableAppIcon(
                    drawable = app.icon,
                    contentDescription = app.label,
                    size = 52.dp,
                    cornerRadius = 14.dp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("pin_app_${app.packageName}"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (app.isPinned) HavalAmber.copy(alpha = 0.2f) else CockpitSurfaceBright.copy(alpha = 0.5f),
                            contentColor = if (app.isPinned) HavalAmber else TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = if (app.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (app.isPinned) "Открепить" else "Закрепить на главном экране",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenInfo,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("info_app_${app.packageName}"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = CockpitSurfaceBright.copy(alpha = 0.5f),
                            contentColor = TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "О приложении",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = app.category.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetryCyan,
                    maxLines = 1
                )
            }
        }
    }
}
