package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        shape = RoundedCornerShape(22.dp),
        color = CockpitSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val isWide = maxWidth >= 820.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isWide) {
                    // Single compact toolbar row on wide screens so the app grid gets maximum height
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BackButton(onBack = onBack)

                        SearchField(
                            searchQuery = searchQuery,
                            onSearchChange = onSearchChange,
                            modifier = Modifier.widthIn(min = 240.dp, max = 340.dp)
                        )

                        CategoryChipsRow(
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${apps.size} прил.",
                            style = MaterialTheme.typography.labelLarge,
                            color = HavalAmber
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BackButton(onBack = onBack)
                        SearchField(
                            searchQuery = searchQuery,
                            onSearchChange = onSearchChange,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${apps.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = HavalAmber
                        )
                    }
                    CategoryChipsRow(
                        selectedCategory = selectedCategory,
                        onSelectCategory = onSelectCategory,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Grid of large, clear app tiles
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
                        columns = GridCells.Adaptive(minSize = if (isWide) 210.dp else 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                            AppDrawerItemCard(
                                app = app,
                                isWide = isWide,
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
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Surface(
        onClick = onBack,
        modifier = Modifier
            .height(50.dp)
            .minimumInteractiveComponentSize()
            .testTag("drawer_back_button"),
        shape = RoundedCornerShape(14.dp),
        color = CockpitSurfaceElevated,
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Назад на главный экран",
                tint = TextPrimary
            )
            Text(
                text = "Назад",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        color = CockpitSurfaceElevated,
        border = BorderStroke(1.dp, CockpitHairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = HavalAmber,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Поиск приложений…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(HavalAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_search_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Очистить поиск",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    selectedCategory: AppCategory,
    onSelectCategory: (AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
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
                    .height(46.dp)
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
}

@Composable
private fun AppDrawerItemCard(
    app: InstalledApp,
    isWide: Boolean,
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
        color = CockpitSurfaceElevated.copy(alpha = 0.96f),
        border = BorderStroke(
            width = if (app.isPinned) 1.5.dp else 1.dp,
            color = if (app.isPinned) HavalAmber else CockpitHairline
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Secondary Pin + Info buttons in top-right corner
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("pin_app_${app.packageName}"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (app.isPinned) HavalAmber.copy(alpha = 0.22f) else CockpitSurfaceBright.copy(alpha = 0.55f),
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
                        containerColor = CockpitSurfaceBright.copy(alpha = 0.55f),
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

            // Large Icon + Clear Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DrawableAppIcon(
                    drawable = app.icon,
                    contentDescription = app.label,
                    size = if (isWide) 80.dp else 68.dp,
                    cornerRadius = 18.dp
                )

                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isWide) 18.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitSurfaceBright.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (app.isPinned) "На главном • ${app.category.title}" else app.category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.isPinned) HavalAmber else TelemetryCyan,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
