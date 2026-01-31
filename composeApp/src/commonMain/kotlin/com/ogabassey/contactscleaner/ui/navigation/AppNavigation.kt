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
import com.ogabassey.contactscleaner.ui.theme.CleanContactsAITheme
import com.ogabassey.contactscleaner.ui.tools.SensitiveReviewScreen
import com.ogabassey.contactscleaner.ui.settings.SafeListScreen
import com.ogabassey.contactscleaner.ui.history.HistoryScreen
import com.ogabassey.contactscleaner.ui.category.CategoryDetailScreen
import com.ogabassey.contactscleaner.ui.duplicates.DuplicateSubGroupsScreen
import com.ogabassey.contactscleaner.ui.duplicates.CrossAccountDetailScreen
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkScreen
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppContactsScreen
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using @Serializable (2026 KMP Best Practice).
 */
@Serializable object DashboardRoute
@Serializable object ResultsRoute
@Serializable object RecentActionsRoute
@Serializable object SafeListRoute
@Serializable object ReviewSensitiveRoute
@Serializable object PaywallRoute
@Serializable object DuplicateGroupsRoute
@Serializable object CrossAccountRoute
@Serializable object HistoryRoute
@Serializable object WhatsAppLinkRoute
@Serializable object WhatsAppContactsRoute
@Serializable data class CategoryDetailRoute(val typeName: String)

/**
 * Main navigation host for the app.
 * Uses type-safe navigation with @Serializable routes (2026 KMP Best Practice).
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    CleanContactsAITheme {
        NavHost(
            navController = navController,
            startDestination = DashboardRoute
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToResults = { navController.navigate(ResultsRoute) },
                    onNavigateToRecentActions = { navController.navigate(RecentActionsRoute) },
                    onNavigateToSafeList = { navController.navigate(SafeListRoute) },
                    onNavigateToReviewSensitive = { navController.navigate(ReviewSensitiveRoute) }
                )
            }

            composable<ResultsRoute> {
                ResultsScreen(
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
                    onNavigateBack = { navController.popBackStack() }
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
