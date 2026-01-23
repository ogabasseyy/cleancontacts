package com.ogabassey.contactscleaner.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.ui.dashboard.DashboardScreen
import com.ogabassey.contactscleaner.ui.results.ScanResultsScreen
import androidx.navigation.toRoute
import com.ogabassey.contactscleaner.domain.model.ContactType

import kotlinx.serialization.Serializable

@Serializable
object Dashboard

@Serializable
object Results

@Serializable
data class ResultsDetail(val typeName: String)

@Serializable
object NumberStandardizer

@Serializable
object Export

@Serializable
object RecentActions

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Dashboard
    ) {
        composable<Dashboard> {
            DashboardScreen(
                onNavigateToResults = { 
                    navController.navigate(Results)
                },
                onNavigateToRecentActions = {
                    navController.navigate(RecentActions)
                }
            )
        }

        composable<Results> {
             ScanResultsScreen(
                 onNavigateBack = {
                     navController.popBackStack()
                 },
                 onNavigateToDetail = { type ->
                     if (type == ContactType.FORMAT_ISSUE) {
                         navController.navigate(com.ogabassey.contactscleaner.ui.navigation.NumberStandardizer)
                     } else {
                         navController.navigate(ResultsDetail(type.name))
                     }
                 },
                 onNavigateToExport = {
                     navController.navigate(Export)
                 }
             )
        }
        
        composable<ResultsDetail> { backStackEntry ->
            val route: ResultsDetail = backStackEntry.toRoute()
            val type = com.ogabassey.contactscleaner.domain.model.ContactType.valueOf(route.typeName)
            
            com.ogabassey.contactscleaner.ui.results.ScanResultsDetailScreen(
                contactType = type,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<NumberStandardizer> {
            com.ogabassey.contactscleaner.ui.tools.NumberStandardizerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExport = {
                    navController.navigate(Export)
                }
            )
        }

        composable<Export> {
            com.ogabassey.contactscleaner.ui.tools.ExportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<RecentActions> {
            com.ogabassey.contactscleaner.ui.history.RecentActionsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
