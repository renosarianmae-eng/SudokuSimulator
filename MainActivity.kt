package com.sudokusimulator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF182A3A)
private val Teal = Color(0xFF167C80)
private val Paper = Color(0xFFF7F8F4)
private val Coral = Color(0xFFE56B5D)
private val areas = listOf("Factory", "Hospital", "Market")
private val emojis = listOf("🏭", "🏥", "🛒")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SudokuApp() }
    }
}

private enum class Screen { HOME, AREAS, GAME, OVER, WIN }

@Composable
fun SudokuApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("sudoku_simulator", 0) }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var unlocked by remember { mutableIntStateOf(prefs.getInt("unlocked", 1).coerceIn(1, 9)) }
    var level by remember { mutableIntStateOf(1) }
    var hearts by remember { mutableIntStateOf(3) }
    var selected by remember { mutableIntStateOf(-1) }
    var message by remember { mutableStateOf("Choose a cell, then choose a number.") }
    var board by remember { mutableStateOf(List(81) { 0 }) }
    var solution by remember { mutableStateOf(List(81) { 0 }) }
    var givens by remember { mutableStateOf(setOf<Int>()) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wordAnswer by remember { mutableStateOf("") }
    var showWordProblem by remember { mutableStateOf(false) }
    var wordIndex by remember { mutableIntStateOf(0) }

    val areaIndex = (level - 1) / 3
    val localLevel = (level - 1) % 3 + 1

    fun startLevel(n: Int) {
        level = n.coerceIn(1, 9)
        hearts = 3
        selected = -1
        message = "Use row, column, and 3×3 box clues. No guessing!"
        correctCount = 0
        wordAnswer = ""
        showWordProblem = false
        wordIndex = 0
        val solved = generateSolvedBoard()
        solution = solved
        val blanks = when ((level - 1) % 3) { 0 -> 34; 1 -> 40; else -> 46 }
        val mutable = solved.toMutableList()
        (0 until 81).shuffled().take(blanks).forEach { mutable[it] = 0 }
        board = mutable
        givens = mutable.indices.filter { mutable[it] != 0 }.toSet()
        screen = Screen.GAME
    }

    fun loseHeart(text: String) {
        hearts -= 1
        message = text
        if (hearts <= 0) screen = Screen.OVER
    }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Teal, onPrimary = Color.White, secondary = Coral,
        background = Paper, surface = Color.White, onSurface = Ink
    )) {
        Surface(Modifier.fillMaxSize(), color = Paper) {
            when (screen) {
                Screen.HOME -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SUDOKU", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Teal)
                    Text("SIMULATOR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    Text("Think. Strategize. Solve.", fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.height(28.dp))
                    Text("🧠", fontSize = 66.sp)
                    Text("A puzzle adventure with math challenges", textAlign = TextAlign.Center, color = Ink)
                    Spacer(Modifier.height(28.dp))
                    Button(onClick = { screen = Screen.AREAS }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Text("START GAME", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = {
                        message = "Fill every row, column, and 3×3 box with 1–9 without repeating. Use elimination, not guessing."
                        screen = Screen.AREAS
                    }, modifier = Modifier.fillMaxWidth()) { Text("HOW TO PLAY") }
                    Spacer(Modifier.height(8.dp))
                    Text("Offline game • 9 levels • 3 hearts", color = Color.Gray, fontSize = 13.sp)
                }

                Screen.AREAS -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text("CHOOSE YOUR MISSION", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(5.dp))
                    Text("Finish levels in order to unlock the next ones.", color = Color.Gray)
                    Spacer(Modifier.height(18.dp))
                    areas.forEachIndexed { i, name ->
                        Card(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFDDE7E4))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("${emojis[i]}  $name", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    when (i) {
                                        0 -> "Logic on the production line"
                                        1 -> "Solve to help the clinic"
                                        else -> "Numbers for smart shopping"
                                    }, color = Color.Gray
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (i * 3 + 1..i * 3 + 3).forEach { n ->
                                        val open = n <= unlocked
                                        OutlinedButton(
                                            onClick = { if (open) startLevel(n) },
                                            enabled = open,
                                            modifier = Modifier.weight(1f)
                                        ) { Text(if (open) "Level $n" else "🔒 $n") }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { screen = Screen.HOME }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to Home")
                    }
                }

                Screen.GAME -> Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${emojis[areaIndex]} ${areas[areaIndex]} • Level $level", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("❤️".repeat(hearts.coerceAtLeast(0)), fontSize = 18.sp)
                    }
                    Text(message, color = if (message.startsWith("Correct")) Teal else Ink, fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(9),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        userScrollEnabled = false
                    ) {
                        itemsIndexed(board) { index, value ->
                            val row = index / 9
                            val col = index % 9
                            val bg = when {
                                index in givens -> Color(0xFFE9EFEE)
                                index == selected -> Color(0xFFBDE7DE)
                                else -> Color.White
                            }
                            Box(
                                Modifier.aspectRatio(1f).background(bg).clickable { selected = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (value == 0) "" else value.toString(),
                                    color = if (index in givens) Ink else Teal,
                                    fontWeight = if (index in givens) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Canvas(Modifier.fillMaxSize()) {
                                    val thin = 0.6f
                                    val thick = 2f
                                    drawLine(Color(0xFFB7C8C7), Offset(0f, size.height), Offset(size.width, size.height),
                                        if ((row + 1) % 3 == 0) thick else thin)
                                    drawLine(Color(0xFFB7C8C7), Offset(size.width, 0f), Offset(size.width, size.height),
                                        if ((col + 1) % 3 == 0) thick else thin)
                                    if (col % 3 == 0) drawLine(Color(0xFF718C8A), Offset(0f, 0f), Offset(0f, size.height), thick)
                                    if (row % 3 == 0) drawLine(Color(0xFF718C8A), Offset(0f, 0f), Offset(size.width, 0f), thick)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                        (1..9).forEach { num ->
                            Button(
                                onClick = {
                                    if (selected < 0) {
                                        message = "Tap a cell first."
                                    } else if (selected in givens || board[selected] != 0) {
                                        message = "Choose an empty cell."
                                    } else if (solution[selected] == num) {
                                        val updated = board.toMutableList()
                                        updated[selected] = num
                                        board = updated
                                        correctCount++
                                        message = "Correct! Great deduction."
                                        if (correctCount % 5 == 0 && hearts < 5) hearts++
                                        if (board.all { it != 0 }) {
                                            unlocked = maxOf(unlocked, minOf(9, level + 1))
                                            prefs.edit().putInt("unlocked", unlocked).apply()
                                            screen = Screen.WIN
                                        }
                                    } else {
                                        loseHeart("Not quite. Recheck the row, column, and box.")
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("$num", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showWordProblem = !showWordProblem; message = "Solve the math challenge for a bonus heart." },
                            modifier = Modifier.weight(1f)
                        ) { Text("Math Challenge", fontSize = 12.sp) }
                        OutlinedButton(onClick = { screen = Screen.AREAS }, modifier = Modifier.weight(1f)) {
                            Text("Exit Level", fontSize = 12.sp)
                        }
                    }
                    if (showWordProblem) {
                        val questions = listOf(
                            Triple("A factory packs 6 boxes with 4 parts each. How many parts?", "24", "6 × 4"),
                            Triple("A hospital has 18 masks and uses 7. How many remain?", "11", "18 − 7"),
                            Triple("A shopper buys 3 items costing ₱12 each. Total cost?", "36", "3 × 12")
                        )
                        val q = questions[(level + wordIndex) % questions.size]
                        Text(q.first, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = wordAnswer,
                                onValueChange = { wordAnswer = it.filter(Char::isDigit) },
                                label = { Text("Answer") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                if (wordAnswer == q.second) {
                                    if (hearts < 5) hearts++
                                    message = "Correct math answer! Bonus heart earned."
                                    wordIndex++
                                } else loseHeart("Incorrect math answer. One heart lost.")
                                wordAnswer = ""
                            }) { Text("Check") }
                        }
                        Text("Hint: ${q.third}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Screen.OVER -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME OVER", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Coral)
                    Text("Out of hearts! Every mistake is a chance to learn.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    Button(onClick = { startLevel(level) }, modifier = Modifier.fillMaxWidth()) { Text("RETRY LEVEL") }
                    OutlinedButton(onClick = { screen = Screen.AREAS }, modifier = Modifier.fillMaxWidth()) { Text("QUIT TO LEVELS") }
                }

                Screen.WIN -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LEVEL COMPLETE!", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Teal)
                    Text("You used logic and elimination to solve the puzzle.", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { if (level < 9) startLevel(level + 1) else screen = Screen.AREAS },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (level < 9) "NEXT LEVEL" else "BACK TO MISSIONS") }
                    OutlinedButton(onClick = { screen = Screen.AREAS }, modifier = Modifier.fillMaxWidth()) { Text("LEVEL SELECT") }
                }
            }
        }
    }
}

private fun generateSolvedBoard(): List<Int> {
    val bandOrder = (0..2).shuffled()
    val rowOrder = bandOrder.flatMap { band -> (0..2).shuffled().map { band * 3 + it } }
    val stackOrder = (0..2).shuffled()
    val colOrder = stackOrder.flatMap { stack -> (0..2).shuffled().map { stack * 3 + it } }
    val nums = (1..9).shuffled()
    return rowOrder.flatMap { r ->
        colOrder.map { c -> nums[(r * 3 + r / 3 + c) % 9] }
    }
}
