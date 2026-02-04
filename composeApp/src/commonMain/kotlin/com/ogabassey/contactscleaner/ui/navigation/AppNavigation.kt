package com.ogabassey.contactscleaner.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.dashboard.DashboardScreen
import com.ogabassey.contactscleaner.ui.history.RecentActionsScreen
import com.ogabassey.contactscleaner.ui.paywall.PaywallScreen
import com.ogabassey.contactscleaner.ui.results.ResultsScreen
import com.ogabassey.contactscleaner.ui.theme.PrimaryNeon
import com.ogabassey.contactscleaner.ui.theme.ContactsCleanerTheme
import com.ogabassey.contactscleaner.ui.tools.SensitiveReviewScreen
import com.ogabassey.contactscleaner.ui.settings.SafeListScreen
import com.ogabassey.contactscleaner.ui.history.HistoryScreen
import com.ogabassey.contactscleaner.ui.category.CategoryDetailScreen
import com.ogabassey.contactscleaner.ui.duplicates.DuplicateSubGroupsScreen
import com.ogabassey.contactscleaner.ui.duplicates.CrossAccountDetailScreen
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkScreen
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppContactsScreen
import com.ogabassey.contactscleaner.ui.limitedaccess.LimitedAccessScreen
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using @Serializable (2026 KMP Best Practice).
 */
@Serializable object DashboardRoute
@Serializable data class ResultsRoute(val autoRescan: Boolean = false)
@Serializable object RecentActionsRoute
@Serializable object SafeListRoute
@Serializable object ReviewSensitiveRoute
@Serializable object PaywallRoute
@Serializable object DuplicateGroupsRoute
@Serializable object CrossAccountRoute
@Serializable object HistoryRoute
@Serializable object WhatsAppLinkRoute
@Serializable object WhatsAppContactsRoute
@Serializable object LimitedAccessRoute
@Serializable object DemoModeRoute
@Serializable data class CategoryDetailRoute(val typeName: String)

/**
 * Main navigation host for the app.
 * Uses type-safe navigation with @Serializable routes (2026 KMP Best Practice).
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    ContactsCleanerTheme {
        NavHost(
            navController = navController,
            startDestination = DashboardRoute
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToResults = { navController.navigate(ResultsRoute()) },
                    onNavigateToRecentActions = { navController.navigate(RecentActionsRoute) },
                    onNavigateToSafeList = { navController.navigate(SafeListRoute) },
                    onNavigateToReviewSensitive = { navController.navigate(ReviewSensitiveRoute) },
                    onNavigateToLimitedAccess = { navController.navigate(LimitedAccessRoute) }
                )
            }

            composable<ResultsRoute> { backStackEntry ->
                val route: ResultsRoute = backStackEntry.toRoute()
                ResultsScreen(
                    autoRescan = route.autoRescan,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { contactType ->
                        when (contactType) {
                            ContactType.SENSITIVE -> navController.navigate(ReviewSensitiveRoute)
                            ContactType.DUPLICATE -> navController.navigate(DuplicateGroupsRoute)
                            else -> navController.navigate(CategoryDetailRoute(contactType.name))
                        }
                    },
                    onNavigateToPaywall = { navController.navigate(PaywallRoute) },
                    onNavigateToHistory = { navController.navigate(HistoryRoute) },
                    onNavigateToWhatsAppLink = { navController.navigate(WhatsAppLinkRoute) },
                    onNavigateToWhatsAppContacts = { navController.navigate(WhatsAppContactsRoute) }
                )
            }

            composable<WhatsAppLinkRoute> {
                WhatsAppLinkScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onConnected = { navController.popBackStack() }
                )
            }

            composable<WhatsAppContactsRoute> {
                WhatsAppContactsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLink = { navController.navigate(WhatsAppLinkRoute) }
                )
            }

            composable<HistoryRoute> {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<CategoryDetailRoute> { backStackEntry ->
                val route: CategoryDetailRoute = backStackEntry.toRoute()
                val contactType = try {
                    ContactType.valueOf(route.typeName)
                } catch (e: Exception) {
                    return@composable
                }
                CategoryDetailScreen(
                    type = contactType,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPaywall = { navController.navigate(PaywallRoute) },
                    onNavigateToResultsWithRescan = { 
                        navController.navigate(ResultsRoute(autoRescan = true)) {
                            popUpTo(DashboardRoute) // Clear stack back to dashboard
                        }
                    }
                )
            }

            composable<RecentActionsRoute> {
                RecentActionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<SafeListRoute> {
                SafeListScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ReviewSensitiveRoute> {
                SensitiveReviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSafeList = { navController.navigate(SafeListRoute) }
                )
            }

            composable<PaywallRoute> {
                PaywallScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable<DuplicateGroupsRoute> {
                DuplicateSubGroupsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { contactType ->
                        if (contactType == ContactType.DUP_CROSS_ACCOUNT) {
                            navController.navigate(CrossAccountRoute)
                        } else {
                            navController.navigate(CategoryDetailRoute(contactType.name))
                        }
                    }
                )
            }

            composable<CrossAccountRoute> {
                CrossAccountDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<LimitedAccessRoute> {
                LimitedAccessScreen(
                    onPickContacts = {
                        // Contact picker will be launched from the screen itself
                        // For now, navigate back - actual picker logic handled in screen
                        navController.popBackStack()
                    },
                    onDemoMode = {
                        navController.navigate(DemoModeRoute) {
                            popUpTo(LimitedAccessRoute) { inclusive = true }
                        }
                    },
                    onOpenSettings = {
                        // Settings opening handled by the screen's permissionState.openSettings()
                        navController.popBackStack()
                    }
                )
            }

            composable<DemoModeRoute> {
                // Demo mode uses the same DashboardScreen but with demo data
                DashboardScreen(
                    onNavigateToResults = { navController.navigate(ResultsRoute()) },
                    onNavigateToRecentActions = { navController.navigate(RecentActionsRoute) },
                    onNavigateToSafeList = { navController.navigate(SafeListRoute) },
                    onNavigateToReviewSensitive = { navController.navigate(ReviewSensitiveRoute) },
                    onNavigateToLimitedAccess = { navController.navigate(LimitedAccessRoute) }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryNeon
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}
