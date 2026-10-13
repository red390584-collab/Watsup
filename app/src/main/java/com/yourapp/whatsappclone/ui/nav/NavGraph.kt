package com.yourapp.whatsappclone.ui.nav

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourapp.whatsappclone.data.CobaltManager
import com.yourapp.whatsappclone.data.ConnectionStatus
import com.yourapp.whatsappclone.data.SessionStore
import com.yourapp.whatsappclone.ui.chats.ChatListScreen
import com.yourapp.whatsappclone.ui.conversation.ConversationScreen
import com.yourapp.whatsappclone.ui.link.LinkDeviceScreen
import com.yourapp.whatsappclone.ui.onboarding.DisclaimerScreen
import com.yourapp.whatsappclone.ui.placeholders.CallsScreen
import com.yourapp.whatsappclone.ui.placeholders.CommunitiesScreen
import com.yourapp.whatsappclone.ui.placeholders.UpdatesScreen
import com.yourapp.whatsappclone.ui.settings.SettingsScreen

object Routes {
    const val LOADING = "loading"
    const val DISCLAIMER = "disclaimer"
    const val LINK = "link"
    const val CHATS = "chats"
    const val UPDATES = "updates"
    const val COMMUNITIES = "communities"
    const val CALLS = "calls"
    const val SETTINGS = "settings"
    const val CONVERSATION = "conversation/{chatName}"
    const val CONVERSATION_ARG = "chatName"

    fun conversation(chatName: String): String = "conversation/${Uri.encode(chatName)}"
}

/**
 * App root: decides the start destination from persisted state (disclaimer
 * acceptance) and hosts the navigation graph.
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val startRoute by produceState(initialValue = Routes.LOADING) {
        val store = SessionStore.get(context)
        value = if (store.disclaimerAccepted()) Routes.CHATS else Routes.DISCLAIMER
    }

    if (startRoute == Routes.LOADING) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
        }
        return
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startRoute) {

        composable(Routes.DISCLAIMER) {
            DisclaimerScreen(
                onAccepted = {
                    navController.navigate(Routes.LINK) {
                        popUpTo(Routes.DISCLAIMER) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LINK) {
            LinkDeviceScreen(
                onLinked = {
                    navController.navigate(Routes.CHATS) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CHATS) {
            val connectionState by CobaltManager.connectionState.collectAsState()
            LaunchedEffect(connectionState) {
                if (connectionState is ConnectionStatus.NeedsLink) {
                    navController.navigate(Routes.LINK) { popUpTo(0) { inclusive = true } }
                }
            }
            ChatListScreen(
                navController = navController,
                onOpenChat = { name ->
                    navController.navigate(Routes.conversation(name))
                },
            )
        }

        composable(Routes.UPDATES) {
            UpdatesScreen(navController = navController)
        }

        composable(Routes.COMMUNITIES) {
            CommunitiesScreen(navController = navController)
        }

        composable(Routes.CALLS) {
            CallsScreen(navController = navController)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                onLoggedOut = {
                    navController.navigate(Routes.LINK) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.CONVERSATION,
            arguments = listOf(navArgument(Routes.CONVERSATION_ARG) {
                type = NavType.StringType
            }),
        ) { entry ->
            val chatName = Uri.decode(entry.arguments?.getString(Routes.CONVERSATION_ARG) ?: "")
            ConversationScreen(
                chatName = chatName,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
