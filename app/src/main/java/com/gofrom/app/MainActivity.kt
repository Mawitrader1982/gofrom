package com.gofrom.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = Color(0xFF83D445)
private val Bg = Color(0xFF071014)
private val Panel = Color(0xFF121C20)
private val Panel2 = Color(0xFF182327)
private val Soft = Color(0xFFA9B2B5)
private val Purple = Color(0xFFB88BDD)
private val Blue = Color(0xFF65B9E8)

private enum class Screen { Welcome, Home, Workouts, Nutrition, Voice, Insights, Progress, Health, Profile }
private enum class Tab(val label: String, val icon: ImageVector, val screen: Screen) {
    Home("Home", Icons.Default.Home, Screen.Home), Workouts("Workouts", Icons.Default.FitnessCenter, Screen.Workouts),
    Nutrition("Nutrition", Icons.Default.Restaurant, Screen.Nutrition), Progress("Progress", Icons.Default.ShowChart, Screen.Progress),
    Profile("Profile", Icons.Default.Person, Screen.Profile)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GoFromTheme { GoFromApp() } }
    }
}

@Composable private fun GoFromTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = Green, background = Bg, surface = Panel, onPrimary = Color.White), content = content
)

@Composable private fun GoFromApp() {
    var screen by remember { mutableStateOf(Screen.Welcome) }
    Scaffold(containerColor = Bg, bottomBar = {
        if (screen != Screen.Welcome && screen != Screen.Voice) BottomNav(screen) { screen = it }
    }) { inset -> Box(Modifier.padding(inset).fillMaxSize().background(Bg)) {
        when (screen) {
            Screen.Welcome -> Welcome { screen = Screen.Home }
            Screen.Home -> HomeScreen { screen = it }
            Screen.Workouts -> WorkoutsScreen()
            Screen.Nutrition -> NutritionScreen { screen = it }
            Screen.Voice -> VoiceScreen({ screen = Screen.Nutrition }) { screen = Screen.Nutrition }
            Screen.Insights -> InsightsScreen()
            Screen.Progress -> ProgressScreen()
            Screen.Health -> HealthScreen { screen = Screen.Profile }
            Screen.Profile -> ProfileScreen { screen = it }
        }
    }}
}

@Composable private fun BottomNav(current: Screen, go: (Screen) -> Unit) {
    NavigationBar(containerColor = Color(0xFF091215), tonalElevation = 0.dp) { Tab.entries.forEach { tab ->
        NavigationBarItem(selected = current == tab.screen || (tab == Tab.Nutrition && current == Screen.Insights) || (tab == Tab.Profile && current == Screen.Health),
            onClick = { go(tab.screen) }, icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = Panel, unselectedIconColor = Soft, unselectedTextColor = Soft))
    }}
}

@Composable private fun Welcome(start: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF111B18)).padding(30.dp)) {
        Column(Modifier.align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("GoFrom", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold); Text("Become the best version of you.", fontSize = 17.sp)
            Feature(Icons.Default.FitnessCenter, "Workouts", "Smart training plans")
            Feature(Icons.Default.Restaurant, "Nutrition", "AI food logging & insights")
            Feature(Icons.Default.ShowChart, "Progress", "Track. Improve. Succeed.")
            Button(start, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(Green)) { Text("Get Started", fontWeight = FontWeight.Bold) }
            Text("Already have an account?  Log in", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Soft)
        }
    }
}

@Composable private fun Feature(icon: ImageVector, title: String, sub: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(shape = CircleShape, color = Panel2) { Icon(icon, null, tint = Green, modifier = Modifier.padding(12.dp)) }
    Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(sub, color = Soft, fontSize = 13.sp) }
}

@Composable private fun Page(title: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold); action?.invoke() } }
        item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }
    }
}

@Composable private fun HomeScreen(go: (Screen) -> Unit) = Page("Good morning,\nMaarten!", { Icon(Icons.Default.Notifications, null) }) {
    Surface(color = Color(0xFFF1F3F1), contentColor = Color(0xFF172018), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) {
        Text("Daily Overview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Ring("1,850", "Calories left", .68f, Green); Ring("82 g", "Protein left", .55f, Purple); Ring("6,240", "Steps", .62f, Green)
        }
    }}
    SectionTitle("Today's Plan", "View all") { go(Screen.Workouts) }
    PlanRow(Icons.Default.FitnessCenter, "Upper Body Strength", "45 min", true) { go(Screen.Workouts) }
    PlanRow(Icons.Default.Timer, "Plank", "1 min", true) { go(Screen.Workouts) }
    PlanRow(Icons.Default.DirectionsWalk, "10K Steps", "6,240 / 10,000", false) { go(Screen.Progress) }
    Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Restaurant, "Log Food", Modifier.weight(1f)) { go(Screen.Nutrition) }; Quick(Icons.Default.FitnessCenter, "Start Workout", Modifier.weight(1f)) { go(Screen.Workouts) } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Mic, "Voice Log", Modifier.weight(1f)) { go(Screen.Voice) }; Quick(Icons.Default.ShowChart, "Progress", Modifier.weight(1f)) { go(Screen.Progress) } }
}

@Composable private fun Ring(value: String, label: String, progress: Float, color: Color) = Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) { drawArc(Color(0xFFD6DCD7), -90f, 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)); drawArc(color, -90f, progress * 360, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(label, fontSize = 9.sp, textAlign = TextAlign.Center) }
}

@Composable private fun SectionTitle(title: String, link: String, click: () -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); Text(link, color = Green, modifier = Modifier.clickable(onClick = click)) }
@Composable private fun PlanRow(icon: ImageVector, title: String, sub: String, done: Boolean, click: () -> Unit) = Surface(Modifier.fillMaxWidth().clickable(onClick = click), color = Panel, shape = RoundedCornerShape(13.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Green, modifier = Modifier.size(38.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(sub, color = Soft, fontSize = 12.sp) }; Icon(if (done) Icons.Default.CheckCircle else Icons.Default.DonutLarge, null, tint = Green) } }
@Composable private fun Quick(icon: ImageVector, label: String, modifier: Modifier, click: () -> Unit) = Surface(modifier.clickable(onClick = click), color = Panel2, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (label == "Voice Log") Color.White else Green); Spacer(Modifier.width(9.dp)); Text(label, fontWeight = FontWeight.SemiBold) } }

@Composable private fun WorkoutsScreen() = Page("Workouts") {
    var rows by remember { mutableStateOf(listOf("Chest Press" to Pair("60", "10"), "Lat Pulldown" to Pair("55", "9"), "Shoulder Press" to Pair("18", "8"), "Triceps Pushdown" to Pair("32", "10"))) }
    Text("Upper Body Strength", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("10 good reps → increase next workout", color = Green)
    rows.forEachIndexed { index, (name, values) -> Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) {
        Text(name, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(values.first, { v -> rows = rows.toMutableList().also { it[index] = name to (v to values.second) } }, Modifier.weight(1f), label = { Text("kg") }, singleLine = true)
            OutlinedTextField(values.second, { v -> rows = rows.toMutableList().also { it[index] = name to (values.first to v) } }, Modifier.weight(1f), label = { Text("Reps") }, singleLine = true)
        }; Text(if ((values.second.toIntOrNull() ?: 0) >= 10) "Ready to progress" else "Keep current weight", color = if ((values.second.toIntOrNull() ?: 0) >= 10) Green else Soft)
    }} }
    PlanRow(Icons.Default.Timer, "Plank", "60 seconds", false) {}
    Button({}, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text("Complete Workout") }
}

@Composable private fun NutritionScreen(go: (Screen) -> Unit) = Page("Nutrition", { Icon(Icons.Default.CalendarMonth, null) }) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("‹"); Text("Today, 1 Jun", fontWeight = FontWeight.Bold); Text("›") } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { Text("Log", color = Green); Text("Meals"); Text("Insights", modifier = Modifier.clickable { go(Screen.Insights) }) }
    Text("Calories", fontWeight = FontWeight.Bold); LinearProgressIndicator(.75f, Modifier.fillMaxWidth(), color = Green); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("1,650 / 2,200 kcal"); Text("550 kcal left", color = Soft) }
    listOf("Breakfast" to "Greek Yoghurt with Berries", "Lunch" to "Grilled Chicken Salad", "Dinner" to "Salmon with Quinoa & Veg", "Snacks" to "Protein Shake").forEach { (meal, food) -> FoodRow(meal, food) }
    Button({ go(Screen.Voice) }, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(Green)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("Log Food") }
}

@Composable private fun FoodRow(meal: String, food: String) { var edit by remember { mutableStateOf(false) }; var value by remember { mutableStateOf(food) }; Column { Text(meal, fontWeight = FontWeight.Bold); Surface(Modifier.fillMaxWidth().clickable { edit = true }, color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Restaurant, null, tint = Green); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(value); Text("Tap to edit", color = Soft, fontSize = 11.sp) }; Icon(Icons.Default.Add, null, tint = Green) } }; if (edit) OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), trailingIcon = { IconButton({ edit = false }) { Icon(Icons.Default.Check, null) } }) } }

@Composable private fun VoiceScreen(close: () -> Unit, saved: () -> Unit) {
    var text by remember { mutableStateOf("") }; val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty() }
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Voice Food Logging", fontSize = 22.sp, fontWeight = FontWeight.Bold); IconButton(close) { Icon(Icons.Default.Close, null) } }
        Spacer(Modifier.height(45.dp)); Surface(Modifier.size(190.dp).clickable { launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) }) }, shape = CircleShape, color = Color(0xFF173A20)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, null, tint = Green, modifier = Modifier.size(82.dp)) } }
        Text(if (text.isBlank()) "Tap to start listening…" else "Check what I heard", fontSize = 20.sp, modifier = Modifier.padding(20.dp))
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 4, placeholder = { Text("Tell me what you ate…") })
        Spacer(Modifier.height(16.dp)); Button(saved, Modifier.fillMaxWidth(), enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(Green)) { Text("Save Food Log") }
        Spacer(Modifier.weight(1f)); Text("✦  Powered by AI", color = Green)
    }
}

@Composable private fun InsightsScreen() = Page("Nutrition Insights") {
    Text("This Week", color = Soft); Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(16.dp)) { SectionTitle("Calorie Intake", "1,950 avg") {}; BarChart() } }
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Ring("Macros", "", .74f, Blue); Spacer(Modifier.width(22.dp)); Column { Text("● Protein   120g (25%)", color = Blue); Text("● Carbs      220g (45%)", color = Green); Text("● Fats          80g (30%)", color = Purple) } } }
    Text("Top Foods", fontWeight = FontWeight.Bold); FoodRow("", "Chicken Breast · 4 servings"); FoodRow("", "Greek Yoghurt · 3 servings"); FoodRow("", "Banana · 3 servings")
}

@Composable private fun BarChart() = Row(Modifier.fillMaxWidth().height(145.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) { listOf(.55f,.7f,.86f,.72f,.74f,.73f,.62f).forEachIndexed { i, h -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.width(14.dp).fillMaxHeight(h).background(Green, RoundedCornerShape(3.dp))); Text(listOf("M","T","W","T","F","S","S")[i], fontSize = 10.sp, color = Soft) } } }

@Composable private fun ProgressScreen() = Page("Progress") {
    Text("Your results", fontSize = 20.sp, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("7D","30D","3M","1Y").forEach { AssistChip({}, { Text(it) }) } }
    MetricCard("Strength", "Chest press  50 → 65 kg", "+30%")
    MetricCard("Body weight", "94.2 → 92.4 kg", "−1.8 kg")
    MetricCard("Training", "14 workouts", "92% completed")
    MetricCard("Sleep", "7h 18m average", "Stable")
}
@Composable private fun MetricCard(title: String, value: String, change: String) = Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(title, color = Soft); Text(value, fontWeight = FontWeight.Bold) }; Text(change, color = Green) } }

@Composable private fun HealthScreen(back: () -> Unit) {
    val context = LocalContext.current; val manager = remember { HealthConnectManager(context) }; val scope = rememberCoroutineScope(); var granted by remember { mutableStateOf<Set<String>>(emptySet()) }; var data by remember { mutableStateOf(HealthSnapshot()) }
    val permissionLauncher = rememberLauncherForActivityResult(manager.permissionContract()) { result -> granted = result; scope.launch { data = manager.sync() } }
    LaunchedEffect(Unit) { granted = manager.grantedPermissions(); if (granted.containsAll(manager.permissions)) data = manager.sync() }
    Page("Google Health", { IconButton(back) { Icon(Icons.Default.ArrowBack, null) } }) {
        Icon(Icons.Default.Favorite, null, tint = Green, modifier = Modifier.align(Alignment.CenterHorizontally).size(95.dp))
        Text(if (granted.containsAll(manager.permissions)) "Connected" else "Permission required", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 20.sp)
        data.lastSynced?.let { Text("Last synced: ${DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(it)}", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Soft) }
        listOf("Steps" to data.steps.toString(), "Heart Rate" to (data.heartRate?.let { "$it bpm" } ?: "—"), "Sleep" to "${data.sleepMinutes / 60}h ${data.sleepMinutes % 60}m", "Calories" to "${data.calories} kcal", "Weight" to (data.weightKg?.let { "%.1f kg".format(it) } ?: "—")).forEach { (label, value) -> Surface(color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, color = Green) } } }
        data.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button({ if (!granted.containsAll(manager.permissions)) permissionLauncher.launch(manager.permissions) else scope.launch { data = manager.sync() } }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text(if (granted.containsAll(manager.permissions)) "Sync Now" else "Connect Health Connect") }
    }
}

@Composable private fun ProfileScreen(go: (Screen) -> Unit) = Page("Profile", { Icon(Icons.Default.Settings, null) }) {
    Surface(Modifier.align(Alignment.CenterHorizontally).size(92.dp), shape = CircleShape, color = Panel2) { Icon(Icons.Default.Person, null, modifier = Modifier.padding(18.dp)) }
    Text("Maarten", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Level 12", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Green)
    Text("Goals", fontWeight = FontWeight.Bold); Text("Build muscle"); LinearProgressIndicator(.8f, Modifier.fillMaxWidth(), color = Green); Text("Lose body fat"); LinearProgressIndicator(.85f, Modifier.fillMaxWidth(), color = Green)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Workouts", "48", Modifier.weight(1f)); Metric("Total Time", "32h", Modifier.weight(1f)); Metric("Streak", "14 days", Modifier.weight(1f)) }
    listOf("Achievements", "Measurements", "Settings", "Help & Support").forEach { item -> Surface(Modifier.fillMaxWidth().clickable { if (item == "Measurements") go(Screen.Health) }, color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(item); Icon(Icons.Default.ChevronRight, null) } } }
    Button({ go(Screen.Health) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text("Google Health Sync") }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier) = Surface(modifier, color = Panel, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Soft, fontSize = 11.sp); Text(value, fontWeight = FontWeight.Bold) } }
