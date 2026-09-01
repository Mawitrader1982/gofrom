package com.gofrom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Lime = Color(0xFF8BDD4C)
private val Ink = Color(0xFF07100C)
private val Card = Color(0xFF142019)
private val Muted = Color(0xFFA7B2AB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GoFromApp() }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home), Fitness("Fitness", Icons.Default.FitnessCenter),
    Food("Food", Icons.Default.Restaurant), Sleep("Sleep", Icons.Default.Bedtime),
    Weight("Weight", Icons.Default.MonitorWeight)
}

@Composable private fun GoFromApp() {
    var tab by remember { mutableStateOf(Tab.Home) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Lime, background = Ink, surface = Card)) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Ink) { Tab.entries.forEach { item ->
                NavigationBarItem(tab == item, { tab = item }, { Icon(item.icon, item.title) }, label = { Text(item.title) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, selectedTextColor = Lime, indicatorColor = Card))
            }}
        }) { inset -> Box(Modifier.padding(inset)) {
            when (tab) {
                Tab.Home -> Dashboard { tab = Tab.Fitness }
                Tab.Fitness -> Workout()
                Tab.Food -> Food()
                Tab.Sleep -> Detail("Last night", "7h 34m", "7-day average · 7h 16m")
                Tab.Weight -> Detail("Current weight", "92.4 kg", "Down 0.6 kg this month")
            }
        }}
    }
}

@Composable private fun Page(title: String, body: @Composable ColumnScope.() -> Unit) =
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("GoFrom", color = Lime, fontWeight = FontWeight.Bold) }
        item { Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = body) }
    }

@Composable private fun Dashboard(openWorkout: () -> Unit) = Page("Good morning, Maarten!") {
    Text("Daily overview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("Sleep", "7h 34m", Modifier.weight(1f)); Metric("Steps", "6,240", Modifier.weight(1f)) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("Weight", "92.4 kg", Modifier.weight(1f)); Metric("Protein left", "82 g", Modifier.weight(1f)) }
    Surface(color = Card, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TODAY'S PLAN", color = Lime, fontWeight = FontWeight.Bold); Text("Upper Body Strength", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Machines + dumbbells · 45 min", color = Muted)
        Button(openWorkout, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Lime, Ink)) { Text("Start workout", fontWeight = FontWeight.Bold) }
    }}
    Text("Health Connect ready", color = Lime)
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) =
    Surface(modifier, color = Card, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text(label, color = Muted); Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }

@Composable private fun Workout() = Page("Today's workout") {
    Text("10 good reps → increase next time", color = Lime, fontWeight = FontWeight.Bold)
    listOf("Chest press" to "60 kg × 10 · Ready to progress", "Lat pulldown" to "55 kg × 9 · Stay at 55 kg", "Shoulder press" to "18 kg × 8 · Stay at 18 kg", "Triceps pushdown" to "32 kg × 10 · Ready to progress", "Plank" to "60 seconds").forEach { (name, result) ->
        Surface(color = Card, shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(result, color = if (result.contains("Ready")) Lime else Muted) } }
    }
    Button({}, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Lime, Ink)) { Text("Complete workout") }
}

@Composable private fun Food() = Page("Nutrition") {
    Text("Tell me what you ate", color = Lime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Metric("Calories", "1,650 / 2,200 kcal", Modifier.fillMaxWidth())
    listOf("Breakfast" to "Greek yoghurt with berries", "Lunch" to "Chicken sandwich + banana", "Dinner · planned" to "Spaghetti bolognese").forEach { (meal, food) ->
        Surface(color = Card, shape = RoundedCornerShape(16.dp)) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(meal, color = Lime, fontWeight = FontWeight.Bold); Text(food) } }
    }
}

@Composable private fun Detail(title: String, value: String, detail: String) = Page(title) {
    Surface(color = Card, shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(28.dp)) { Text(value, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold); Text(detail, color = Muted) } }
    Text("Synced through Health Connect", color = Lime)
}
