package com.example.flappybirdnikitakajurins

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flappybirdnikitakajurins.ui.theme.FlappyBirdNikitaKajurinsTheme
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GameState { Start, Playing, GameOver }

data class Pipe(val x: Float, val gapY: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val highScoreRef = Firebase.database.reference.child("highScore")
        setContent {
            FlappyBirdNikitaKajurinsTheme {
                FlappyGameScreen(highScoreRef)
            }
        }
    }
}

@Composable
fun FlappyGameScreen(highScoreRef: DatabaseReference) {
    var playerName by remember { mutableStateOf("") }
    var birdY by remember { mutableStateOf(300f) }
    var velocity by remember { mutableStateOf(0f) }
    var gameState by remember { mutableStateOf(GameState.Start) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }

    // Загрузка рекорда
    LaunchedEffect(Unit) {
        highScoreRef.get().addOnSuccessListener { snapshot ->
            snapshot.getValue(Int::class.java)?.let { highScore = it }
        }
    }

    val gravity = 0.8f
    val jumpForce = -12f
    val pipeWidth = 60f
    val gapHeight = 250f
    val pipeSpeed = 4f
    val birdHeight = 50f
    val birdX = 100f
    val groundHeight = 70f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable { if (gameState == GameState.Playing) velocity = jumpForce }
    ) {
        val density = LocalDensity.current
        val screenHeight = with(density) { constraints.maxHeight.toDp().value }
        val screenWidth = with(density) { constraints.maxWidth.toDp().value }
        val maxBirdY = screenHeight - groundHeight - birdHeight

        // Игровой цикл
        LaunchedEffect(gameState) {
            while (true) {
                if (gameState == GameState.Playing) {
                    velocity += gravity
                    birdY = (birdY + velocity).coerceIn(0f, maxBirdY)
                    if (birdY >= maxBirdY) gameState = GameState.GameOver

                    pipes = pipes.map { it.copy(x = it.x - pipeSpeed) }
                        .filter { it.x + pipeWidth > 0 }

                    if (pipes.isEmpty() || pipes.last().x < screenWidth - 200f) {
                        val minY = 100f
                        val maxY = screenHeight - gapHeight - groundHeight - 100f
                        val gapY = Random.nextFloat() * (maxY - minY) + minY
                        pipes = pipes + Pipe(screenWidth, gapY)
                    }

                    pipes.forEach {
                        if (it.x + pipeWidth < birdX && it.x + pipeWidth >= birdX - pipeSpeed) score++
                    }

                    val pad = 10f
                    val bL = birdX + pad
                    val bR = birdX + birdHeight - pad
                    val bT = birdY + pad
                    val bB = birdY + birdHeight - pad
                    pipes.forEach { pipe ->
                        val pL = pipe.x
                        val pR = pipe.x + pipeWidth
                        val hitX = bR > pL && bL < pR
                        val hitY = bT < pipe.gapY || bB > pipe.gapY + gapHeight
                        if (hitX && hitY) gameState = GameState.GameOver
                    }

                    if (gameState == GameState.GameOver && score > highScore) {
                        highScoreRef.setValue(score)
                        highScore = score
                    }
                }
                delay(16)
            }
        }

        // Фон
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Трубы
        pipes.forEach { pipe ->
            Column(
                modifier = Modifier
                    .offset(x = pipe.x.dp, y = 0.dp)
                    .width(pipeWidth.dp)
                    .height(pipe.gapY.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pipe),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .offset(x = pipe.x.dp, y = (pipe.gapY + gapHeight).dp)
                    .width(pipeWidth.dp)
                    .height((screenHeight - pipe.gapY - gapHeight - groundHeight).dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.pipe),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Птичка
        Image(
            painter = painterResource(id = R.drawable.bluebird),
            contentDescription = null,
            modifier = Modifier
                .offset(x = birdX.dp, y = birdY.dp)
                .size(birdHeight.dp)
        )

        // Земля
        Image(
            painter = painterResource(id = R.drawable.base),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(groundHeight.dp)
                .align(Alignment.BottomCenter)
        )

        // Счёт и рекорд
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            if (gameState == GameState.Playing) {
                Text("Hello, $playerName!", color = Color.White)
            }
            Text("Score: $score", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("High Score: $highScore", color = Color.Yellow)
        }

        // Стартовое меню с вводом имени
        if (gameState == GameState.Start) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Enter your name:", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    placeholder = { Text("Player") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (playerName.isBlank()) playerName = "Player"
                        gameState = GameState.Playing
                    }
                ) {
                    Text("Start")
                }
            }
        }

        // Экран Game Over
        if (gameState == GameState.GameOver) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gameover),
                    contentDescription = null,
                    modifier = Modifier.size(250.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Good try, $playerName!", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    birdY = 300f; velocity = 0f; pipes = emptyList(); score = 0; gameState = GameState.Start
                }) {
                    Text("Restart")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFlappy() {
    FlappyBirdNikitaKajurinsTheme {
        FlappyGameScreen(Firebase.database.reference.child("highScore"))
    }
}
