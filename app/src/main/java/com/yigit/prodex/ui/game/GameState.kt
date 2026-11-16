package com.yigit.prodex.ui.game

import androidx.annotation.DrawableRes
import com.yigit.prodex.R

data class GameState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val score: Int = 0,
    val highScore: Int = 0,
    val coins: Int = 0,
    val player: Player = Player(),
    val obstacles: List<Obstacle> = emptyList(),
    val collectibles: List<Collectible> = emptyList(),
    val gameSpeed: Float = 5f,
    val distance: Float = 0f,
    val germanExclamation: String? = null,
    val exclamationEmoji: String? = null,
    val exclamationTimestamp: Long? = null,
    val snowflakes: List<Snowflake> = emptyList(),
    val raindrops: List<Raindrop> = emptyList(),
    val lightnings: List<Lightning> = emptyList(),
    val lastLightningTime: Long = 0,
    val lastObstacleType: ObstacleType? = null,
    val lastCollectibleType: CollectibleType? = null
)

data class Snowflake(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float
)

data class Raindrop(
    val x: Float,
    val y: Float,
    val speed: Float,
    val length: Float
)

data class Lightning(
    val x: Float,
    val startY: Float,
    val endY: Float,
    val timestamp: Long,
    val duration: Long = 150 // milliseconds
)

data class Player(
    val x: Float = 100f,
    val y: Float = 0f,
    val velocityY: Float = 0f,
    val isJumping: Boolean = false,
    val character: GameCharacter = GameCharacter.TOURIST
)

enum class GameCharacter(
    val displayName: String,
    val emoji: String,
    val speed: Float,
    val description: String,
    @DrawableRes val imageRes: Int
) {
    TOURIST("Tourist", "🧊", 1.3f, "Freezing tourist exploring Berlin", R.drawable.ic_tourist_2),
    ROCKER("Rocker", "🎸", 1.7f, "Cool rocker in leather jacket", R.drawable.ic_rocker_2)
}

sealed class GameObject(
    open val x: Float,
    open val y: Float,
    open val width: Float,
    open val height: Float
)

data class Obstacle(
    override val x: Float,
    override val y: Float,
    override val width: Float = 120f,
    override val height: Float = 120f,
    val type: ObstacleType
) : GameObject(x, y, width, height)

enum class ObstacleType(val emoji: String, @DrawableRes val imageRes: Int? = null) {
    BAUSTELLE("🚧", null),
    UBAHN("🚇", null),
    SBAHN("🚊", null),
    DB_BAHN("🚄", null),
    TRABANT("🚗", null),
    BIKE("🚴", null),
    SCOOTER("🛴", null),
    FOX("🦊", null),
    TOURIST_GROUP("👥", null),
    BEAR("🐻", null),
    WINTER("❄️", null)
}

data class Collectible(
    override val x: Float,
    override val y: Float,
    override val width: Float = 80f,
    override val height: Float = 80f,
    val type: CollectibleType,
    val isCollected: Boolean = false
) : GameObject(x, y, width, height)

enum class CollectibleType(val emoji: String, val points: Int, @DrawableRes val imageRes: Int? = null) {
    PRETZEL("🥨", 10, null),
    BEER("🍺", 20, null),
    CURRYWURST("🌭", 15, null),
    DONER("🥙", 30, null),
    GLUHWEIN("🍷", 35, null),
    VINYL("🎵", 25, null)
}
