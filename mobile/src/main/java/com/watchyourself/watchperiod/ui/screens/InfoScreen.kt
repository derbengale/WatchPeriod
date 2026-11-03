package com.watchyourself.watchperiod.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchyourself.watchperiod.domain.PartnerInfo
import com.watchyourself.watchperiod.domain.PmddInfo
import com.watchyourself.watchperiod.domain.Sources
import com.watchyourself.watchperiod.ui.InfoPage
import com.watchyourself.watchperiod.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    page: InfoPage,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(page.title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹", fontSize = 30.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (page) {
                InfoPage.PARTNER -> {
                    item {
                        SectionCard {
                            Text(PartnerInfo.headline, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Konkrete Hinweise für eine unterstützende Rolle – ohne Kontrolle und ohne Schuldzuweisung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        Text(
                            PartnerInfo.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PartnerInfo.sections.forEach { section ->
                        item {
                            SectionCard(title = section.title) {
                                Text(section.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                InfoPage.PMDD -> {
                    item {
                        Text(
                            PmddInfo.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PmddInfo.sections.forEach { section ->
                        item {
                            SectionCard(title = section.title) {
                                Text(section.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                InfoPage.SOURCES -> {
                    item {
                        Text(
                            Sources.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        SectionCard(title = "Quellen") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Sources.items.forEach { source ->
                                    Text(source, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
