package com.yigit.prodex.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameLoopJob: Job? = null
    private val gravity = 0.6f
    private val jumpForce = -15f
    private val groundLevel = 400f

    fun startGame(character: GameCharacter) {
        // Create snowflakes for Tourist character
        val snowflakes = if (character == GameCharacter.TOURIST) {
            List(30) {
                Snowflake(
                    x = Random.nextFloat() * 1200f,
                    y = Random.nextFloat() * 800f,
                    speed = Random.nextFloat() * 2f + 1f,
                    size = Random.nextFloat() * 8f + 4f
                )
            }
        } else emptyList()

        // Create raindrops for Rocker character
        val raindrops = if (character == GameCharacter.ROCKER) {
            List(50) {
                Raindrop(
                    x = Random.nextFloat() * 1200f,
                    y = Random.nextFloat() * 800f,
                    speed = Random.nextFloat() * 8f + 5f,
                    length = Random.nextFloat() * 20f + 10f
                )
            }
        } else emptyList()

        _gameState.value = GameState(
            isPlaying = true,
            player = Player(character = character, y = groundLevel),
            highScore = _gameState.value.highScore,
            snowflakes = snowflakes,
            raindrops = raindrops,
            lastLightningTime = System.currentTimeMillis()
        )
        startGameLoop()
    }

    fun jump() {
        if (_gameState.value.isPlaying && !_gameState.value.isPaused) {
            val player = _gameState.value.player
            if (!player.isJumping) {
                _gameState.value = _gameState.value.copy(
                    player = player.copy(
                        velocityY = jumpForce,
                        isJumping = true
                    )
                )
            }
        }
    }

    fun pauseGame() {
        _gameState.value = _gameState.value.copy(isPaused = true)
    }

    fun resumeGame() {
        _gameState.value = _gameState.value.copy(isPaused = false)
    }

    fun restartGame() {
        val character = _gameState.value.player.character
        val highScore = _gameState.value.highScore
        stopGameLoop()
        _gameState.value = GameState(highScore = highScore)
        startGame(character)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_gameState.value.isPlaying && !_gameState.value.isGameOver) {
                if (!_gameState.value.isPaused) {
                    updateGame()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private fun updateGame() {
        var state = _gameState.value
        val speed = state.gameSpeed * state.player.character.speed

        // Update player physics
        var player = state.player
        player = player.copy(velocityY = player.velocityY + gravity)
        player = player.copy(y = player.y + player.velocityY)

        // Ground collision
        if (player.y >= groundLevel) {
            player = player.copy(
                y = groundLevel,
                velocityY = 0f,
                isJumping = false
            )
        }

        // Spawn obstacles - NO DUPLICATES
        var obstacles = state.obstacles.toMutableList()
        var lastObstacleType = state.lastObstacleType
        if (obstacles.isEmpty() || obstacles.last().x < 800f) {
            if (Random.nextFloat() < 0.02f) {
                // Get different obstacle type than last one
                val availableTypes = ObstacleType.entries.filter { it != lastObstacleType }
                val newType = availableTypes.random()
                lastObstacleType = newType

                obstacles.add(
                    Obstacle(
                        x = 1200f,
                        y = groundLevel,
                        type = newType
                    )
                )
            }
        }

        // Move obstacles
        obstacles = obstacles.map { it.copy(x = it.x - speed) }.toMutableList()
        obstacles.removeAll { it.x < -100f }

        // Spawn collectibles - MORE FREQUENT!
        var collectibles = state.collectibles.toMutableList()
        var lastCollectibleType = state.lastCollectibleType
        if (collectibles.isEmpty() || collectibles.last().x < 400f) {
            if (Random.nextFloat() < 0.08f) { // Much higher spawn rate!
                // Get different collectible type than last one
                val availableTypes = CollectibleType.entries.filter { it != lastCollectibleType }
                val newType = availableTypes.random()
                lastCollectibleType = newType

                val randomY = if (Random.nextBoolean()) groundLevel - 150f else groundLevel
                collectibles.add(
                    Collectible(
                        x = 1200f,
                        y = randomY,
                        type = newType
                    )
                )
            }
        }

        // Move collectibles
        collectibles = collectibles.map { it.copy(x = it.x - speed) }.toMutableList()
        collectibles.removeAll { it.x < -100f }

        // Collision detection - Obstacles
        val playerRect = PlayerRect(player)
        val collision = obstacles.any { obstacle ->
            checkCollision(playerRect, obstacle)
        }

        // Show German exclamation on collision
        if (collision && state.germanExclamation == null) {
            val germanExclamations = listOf(
                // Classic German exclamations
                "Scheiße!",
                "Mist!",
                "Verdammt!",
                "Verflixt!",
                "Verflucht!",

                // Traditional expressions
                "Ach Mensch!",
                "Ach du Schande!",
                "Ach du lieber!",
                "Ach du grüne Neune!",
                "Ach du meine Güte!",
                "Ach du heiliger Strohsack!",
                "Ach herrje!",

                // Strong expressions
                "Donnerwetter!",
                "Himmel!",
                "Himmelherrgott!",
                "Zum Teufel!",
                "Sakrament!",

                // Berlin slang
                "Alter!",
                "Krass!",
                "Ey was?!",
                "Ach nee!",
                "Wat denn nu?!",
                "Ick gloob et nicht!",
                "Menno!",

                // German idioms & sayings
                "Ach du dickes Ei!",
                "Ach du Schreck!",
                "Potzblitz!",
                "Donnerlittchen!",
                "Herr im Himmel!",
                "Du lieber Himmel!",
                "Mein Gott!",
                "Um Himmels willen!",
                "Das gibt's doch nicht!",
                "So ein Mist!",
                "Scheibenkleister!",
                "Verflixt und zugenäht!",
                "Sapperment!"

            )
            _gameState.value = state.copy(
                germanExclamation = germanExclamations.random(),
                exclamationTimestamp = System.currentTimeMillis()
            )
        }

        // Check if exclamation should disappear (after 3 seconds)
        if (state.germanExclamation != null && state.exclamationTimestamp != null) {
            if (System.currentTimeMillis() - state.exclamationTimestamp > 3000) {
                _gameState.value = state.copy(
                    germanExclamation = null,
                    exclamationTimestamp = null
                )
            }
        }

        if (collision) {
            gameOver()
            return
        }

        // Collision detection - Collectibles
        var score = state.score
        collectibles = collectibles.map { collectible ->
            if (!collectible.isCollected && checkCollision(playerRect, collectible)) {
                score += collectible.type.points
                collectible.copy(isCollected = true)
            } else {
                collectible
            }
        }.toMutableList()

        collectibles.removeAll { it.isCollected }

        // Update snowflakes (Tourist only)
        val snowflakes = if (state.player.character == GameCharacter.TOURIST) {
            state.snowflakes.map { flake ->
                val newY = flake.y + flake.speed
                val newX = flake.x - speed * 0.3f
                if (newY > 800f || newX < -50f) {
                    Snowflake(
                        x = if (newX < -50f) 1250f else Random.nextFloat() * 1200f,
                        y = if (newY > 800f) -20f else newY,
                        speed = Random.nextFloat() * 2f + 1f,
                        size = Random.nextFloat() * 8f + 4f
                    )
                } else {
                    flake.copy(x = newX, y = newY)
                }
            }
        } else emptyList()

        // Update raindrops (Rocker only)
        val raindrops = if (state.player.character == GameCharacter.ROCKER) {
            state.raindrops.map { drop ->
                val newY = drop.y + drop.speed
                val newX = drop.x - speed * 0.2f
                if (newY > 800f || newX < -50f) {
                    Raindrop(
                        x = if (newX < -50f) 1250f else Random.nextFloat() * 1200f,
                        y = if (newY > 800f) -20f else newY,
                        speed = Random.nextFloat() * 8f + 5f,
                        length = Random.nextFloat() * 20f + 10f
                    )
                } else {
                    drop.copy(x = newX, y = newY)
                }
            }
        } else emptyList()

        // Update lightning (Rocker only - random every 0.8-1.5 seconds - MORE FREQUENT!)
        val currentTime = System.currentTimeMillis()
        var lightnings = state.lightnings.filter {
            currentTime - it.timestamp < it.duration
        }.toMutableList()

        var lastLightningTime = state.lastLightningTime
        if (state.player.character == GameCharacter.ROCKER) {
            if (currentTime - lastLightningTime > Random.nextLong(800, 1500)) {
                // Add 2-3 lightnings at once for more dramatic effect
                val numLightnings = Random.nextInt(2, 4)
                repeat(numLightnings) {
                    lightnings.add(
                        Lightning(
                            x = Random.nextFloat() * 1200f,
                            startY = 0f,
                            endY = Random.nextFloat() * 500f + 300f,
                            timestamp = currentTime,
                            duration = 200 // Longer duration
                        )
                    )
                }
                lastLightningTime = currentTime
            }
        }

        // Increase difficulty
        val distance = state.distance + speed
        val gameSpeed = 5f + (distance / 1000f) * 0.5f

        // Calculate coins (every 10 points = 1 coin)
        val coins = score / 10

        _gameState.value = state.copy(
            player = player,
            obstacles = obstacles,
            collectibles = collectibles,
            score = score,
            distance = distance,
            gameSpeed = gameSpeed.coerceAtMost(12f),
            coins = coins,
            snowflakes = snowflakes,
            raindrops = raindrops,
            lightnings = lightnings,
            lastLightningTime = lastLightningTime,
            lastObstacleType = lastObstacleType,
            lastCollectibleType = lastCollectibleType
        )
    }

    private fun checkCollision(player: PlayerRect, obj: GameObject): Boolean {
        return player.x < obj.x + obj.width &&
                player.x + player.width > obj.x &&
                player.y < obj.y + obj.height &&
                player.y + player.height > obj.y
    }

    private data class PlayerRect(val x: Float, val y: Float, val width: Float, val height: Float) {
        constructor(player: Player) : this(player.x, player.y, 50f, 50f)
    }

    private fun gameOver() {
        val currentState = _gameState.value
        val newHighScore = maxOf(currentState.score, currentState.highScore)

        _gameState.value = currentState.copy(
            isPlaying = false,
            isGameOver = true,
            highScore = newHighScore
        )
        stopGameLoop()
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
