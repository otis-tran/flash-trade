package com.otistran.flash_trade.presentation.navigation

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.otistran.flash_trade.ui.theme.FlashTradeTheme

/**
 * Bottom navigation bar for Flash Trade app.
 * Stateless component using Material3 NavigationBar.
 *
 * Features:
 * - Kyber brand colors (Teal primary, Navy background)
 * - Icon transitions (outlined ↔ filled)
 * - Label font weight changes (medium → bold)
 * - WCAG AA color contrast compliance
 * - Min 48x48dp touch targets
 */
@Composable
fun BottomNavBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination?,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        destinations.forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    AnimatedContent(
                        targetState = isSelected,
                        label = "icon_animation_${destination.name}"
                    ) { selected ->
                        Icon(
                            imageVector = if (selected) {
                                destination.iconFilled
                            } else {
                                destination.iconOutlined
                            },
                            contentDescription = destination.label
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = if (isSelected) {
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.labelMedium
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BottomNavBarPreview() {
    FlashTradeTheme {
        BottomNavBar(
            destinations = TopLevelDestination.entries,
            currentDestination = TopLevelDestination.HOME,
            onNavigateToDestination = {}
        )
    }
}
