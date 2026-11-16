package com.yigit.prodex.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameMenuScreen(
    currentCoins: Int,
    isRockerUnlocked: Boolean,
    onPlayGame: (GameCharacter) -> Unit,
    onPurchaseRocker: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                " Berlin Runner",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Coins: $currentCoins 🪙",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Spacer(Modifier.height(16.dp))

            // TOURIST Character Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                border = BorderStroke(2.dp, Color(0xFF4CAF50))
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tourist",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Always Unlocked!",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onPlayGame(GameCharacter.TOURIST) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("PLAY AS TOURIST", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ROCKER Character Card
            val isUnlocked = isRockerUnlocked
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                border = BorderStroke(
                    2.dp,
                    if (isUnlocked) Color(0xFF4CAF50) else Color.Gray
                )
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Rocker",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        if (isUnlocked) "Unlocked!" else "100 Coins",
                        fontSize = 14.sp,
                        color = if (isUnlocked) Color(0xFF4CAF50) else Color.Red
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Runs 30% faster! 🎸⚡",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    if (isUnlocked) {
                        Button(
                            onClick = { onPlayGame(GameCharacter.ROCKER) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("PLAY AS ROCKER", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPurchaseRocker,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentCoins >= 100,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(2.dp, Color(0xFFFFD700))
                        ) {
                            Text(
                                "UNLOCK FOR 100 COINS",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(2.dp, Color(0xFF666666))
            ) {
                Text("← BACK TO HOME", fontSize = 16.sp)
            }
        }
    }
}
