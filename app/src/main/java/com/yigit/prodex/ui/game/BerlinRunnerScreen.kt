package com.yigit.prodex.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yigit.prodex.R
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BerlinRunnerScreen(
    character: GameCharacter,
    onGameEnd: (Int) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: GameViewModel = viewModel()
    val gameState by viewModel.gameState.collectAsState()

    val emojiPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    LaunchedEffect(Unit) { viewModel.startGame(character) }

    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            delay(1500)
            onGameEnd(gameState.coins)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB))) {
        if (!gameState.isGameOver) {
            val characterBitmap = ImageBitmap.imageResource(gameState.player.character.imageRes).asAndroidBitmap()
            val berlinBackground = ImageBitmap.imageResource(R.drawable.ic_berlin).asAndroidBitmap()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { if (!gameState.isPaused) viewModel.jump() }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val groundY = canvasHeight * 0.70f // Ground level

                // 1. FULL SKY (To prevent grey gaps)
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF40E0D0), Color(0xFF87CEEB), Color(0xFFB0E0E6)),
                        startY = 0f,
                        endY = canvasHeight
                    ),
                    size = Size(canvasWidth, canvasHeight)
                )

                // 2. Berlin Background Image
                drawIntoCanvas { canvas ->
                    val bgPaint = Paint().apply { alpha = 160; isAntiAlias = true }
                    // Stretched to cover width, ends at ground level
                    canvas.nativeCanvas.drawBitmap(
                        berlinBackground,
                        null,
                        android.graphics.RectF(0f, 0f, canvasWidth, groundY),
                        bgPaint
                    )
                }

                // 3. Ground (Asphalt & Cobblestone)
                // Main Asphalt Base
                drawRect(
                    color = Color(0xFF333333),
                    topLeft = Offset(0f, groundY),
                    size = Size(canvasWidth, canvasHeight - groundY)
                )

                // Simplified Cobblestone Texture (More performant)
                val cobbleSize = 40f
                for (yPos in groundY.toInt()..canvasHeight.toInt() step cobbleSize.toInt()) {
                    drawLine(
                        color = Color(0xFF444444),
                        start = Offset(0f, yPos.toFloat()),
                        end = Offset(canvasWidth, yPos.toFloat()),
                        strokeWidth = 2f
                    )
                }

                // 4. Weather Effects
                if (gameState.player.character == GameCharacter.TOURIST) {
                    gameState.snowflakes.forEach { drawCircle(Color.White.copy(alpha = 0.7f), it.size, Offset(it.x, it.y)) }
                }

                if (gameState.player.character == GameCharacter.ROCKER) {
                    gameState.lightnings.forEach { lightning ->
                        var curX = lightning.x
                        var curY = lightning.startY
                        for (i in 0 until 6) {
                            val nextX = curX + Random.nextFloat() * 30f - 15f
                            val nextY = curY + 40f
                            drawLine(Color.Yellow, Offset(curX, curY), Offset(nextX, nextY), 4f)
                            curX = nextX; curY = nextY
                        }
                    }
                }

                // 5. Entities (Character, Obstacles, Collectibles)
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val playerYPos = groundY - (400f - gameState.player.y)

                    // DRAW CHARACTER
                    val destRect = android.graphics.RectF(
                        gameState.player.x, playerYPos - 120f,
                        gameState.player.x + 120f, playerYPos
                    )
                    nativeCanvas.drawBitmap(characterBitmap, null, destRect, Paint().apply { isFilterBitmap = true })

                    // DRAW COLLECTIBLES (The missing part!)
                    emojiPaint.textSize = 75f
                    gameState.collectibles.forEach { collectible ->
                        val collY = groundY - (400f - collectible.y)
                        nativeCanvas.drawText(collectible.type.emoji, collectible.x + 40f, collY, emojiPaint)
                    }

                    // DRAW OBSTACLES
                    emojiPaint.textSize = 70f
                    gameState.obstacles.forEach { obs ->
                        val obsY = groundY - (400f - obs.y)
                        nativeCanvas.drawText(obs.type.emoji, obs.x + 40f, obsY, emojiPaint)
                    }

                    // EXCLAMATIONS
                    gameState.germanExclamation?.let { text ->
                        emojiPaint.textSize = 55f
                        emojiPaint.style = Paint.Style.STROKE
                        emojiPaint.strokeWidth = 8f
                        emojiPaint.color = android.graphics.Color.WHITE
                        nativeCanvas.drawText(text, gameState.player.x + 60f, playerYPos - 130f, emojiPaint)

                        emojiPaint.style = Paint.Style.FILL
                        emojiPaint.color = android.graphics.Color.RED
                        nativeCanvas.drawText(text, gameState.player.x + 60f, playerYPos - 130f, emojiPaint)
                    }
                }
            }

            HUDSection(gameState, viewModel)

        } else {
            GameOverScreen(gameState, viewModel, onBack)
        }
    }
}

@Composable
fun HUDSection(gameState: GameState, viewModel: GameViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Button(
            onClick = { if (gameState.isPaused) viewModel.resumeGame() else viewModel.pauseGame() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Text(if (gameState.isPaused) "▶️ Resume" else "⏸️ Pause", color = Color.White)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Score: ${gameState.score}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Coins: ${gameState.coins} 🪙", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
fun GameOverScreen(gameState: GameState, viewModel: GameViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252525))
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("GAME OVER", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                HorizontalDivider(color = Color(0xFF333333))
                Text("Score: ${gameState.score}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Best: ${gameState.highScore}", color = Color(0xFFFFD700))

                Button(
                    onClick = { viewModel.restartGame() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Try Again") }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) { Text("Menu", color = Color.White) }
            }
        }
    }
}