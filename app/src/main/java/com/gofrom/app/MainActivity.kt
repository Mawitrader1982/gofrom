package com.gofrom.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val Green = Color(0xFF83D445)
private val Bg = Color(0xFF071014)
private val Panel = Color(0xFF121C20)
private val Panel2 = Color(0xFF182327)
private val Soft = Color(0xFFA9B2B5)
private val Purple = Color(0xFFB88BDD)
private val Blue = Color(0xFF65B9E8)

private enum class Screen { Welcome, Login, Home, Workouts, Nutrition, Meals, Voice, Insights, Progress, Health, Profile, EditProfile }
private enum class ExerciseEquipment(val label: String) {
    MACHINE("Machine"), CABLE("Cable"), DUMBBELL("Dumbbells"), BODYWEIGHT("Bodyweight")
}
private enum class ExerciseVisual {
    CHEST_PRESS, INCLINE_PRESS, SHOULDER_PRESS, DUMBBELLS, CABLE_PUSH,
    LAT_PULLDOWN, SEATED_ROW, DUMBBELL_ROW, PEC_DECK, LEG_PRESS, LEG_CURL,
    LEG_EXTENSION, HIP_THRUST, CALF_RAISE, PLANK
}
private data class ExercisePlan(
    val name: String,
    val prescription: String,
    val equipment: ExerciseEquipment,
    val visual: ExerciseVisual,
    val usesWeight: Boolean = true,
    val youtubeUrl: String? = null,
)
private data class WorkoutDay(val key: String, val shortLabel: String, val title: String, val focus: String, val exercises: List<ExercisePlan>)

private val workoutPlan = listOf(
    WorkoutDay("monday", "Mon", "Monday · Push", "Chest, shoulders & triceps", listOf(
        ExercisePlan("Chest press", "3 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.CHEST_PRESS),
        ExercisePlan("Incline chest press", "3 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.INCLINE_PRESS),
        ExercisePlan("Shoulder press", "3 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.SHOULDER_PRESS),
        ExercisePlan("Lateral raise", "3 sets × 12–15 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=3VcKaXpzqRo"),
        ExercisePlan("Triceps pushdown", "3 sets × 10–12 reps", ExerciseEquipment.CABLE, ExerciseVisual.CABLE_PUSH),
        ExercisePlan("Overhead triceps extension", "3 sets × 10–12 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=-Vyt2QdsR7E"),
        ExercisePlan("Plank", "3 sets × 60 seconds", ExerciseEquipment.BODYWEIGHT, ExerciseVisual.PLANK, false)
    )),
    WorkoutDay("tuesday", "Tue", "Tuesday · Pull", "Back & biceps", listOf(
        ExercisePlan("Lat pulldown", "3 sets × 8–10 reps", ExerciseEquipment.CABLE, ExerciseVisual.LAT_PULLDOWN),
        ExercisePlan("Seated cable row", "3 sets × 8–10 reps", ExerciseEquipment.CABLE, ExerciseVisual.SEATED_ROW),
        ExercisePlan("Chest-supported row", "3 sets × 8–10 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELL_ROW, youtubeUrl = "https://www.youtube.com/watch?v=_b6ch2nIchk"),
        ExercisePlan("Reverse fly", "3 sets × 12–15 reps", ExerciseEquipment.MACHINE, ExerciseVisual.PEC_DECK),
        ExercisePlan("Biceps curl", "3 sets × 10–12 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=in7PaeYlhrM"),
        ExercisePlan("Hammer curl", "3 sets × 10–12 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=BRVDS6HVR9Q"),
        ExercisePlan("Plank", "3 sets × 60 seconds", ExerciseEquipment.BODYWEIGHT, ExerciseVisual.PLANK, false)
    )),
    WorkoutDay("thursday", "Thu", "Thursday · Legs", "Legs & glutes", listOf(
        ExercisePlan("Leg press", "4 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.LEG_PRESS),
        ExercisePlan("Leg curl", "3 sets × 10–12 reps", ExerciseEquipment.MACHINE, ExerciseVisual.LEG_CURL),
        ExercisePlan("Leg extension", "3 sets × 10–12 reps", ExerciseEquipment.MACHINE, ExerciseVisual.LEG_EXTENSION),
        ExercisePlan("Hip thrust", "3 sets × 8–12 reps", ExerciseEquipment.MACHINE, ExerciseVisual.HIP_THRUST),
        ExercisePlan("Calf raise", "3 sets × 12–15 reps", ExerciseEquipment.MACHINE, ExerciseVisual.CALF_RAISE),
        ExercisePlan("Plank", "3 sets × 60 seconds", ExerciseEquipment.BODYWEIGHT, ExerciseVisual.PLANK, false)
    )),
    WorkoutDay("friday", "Fri", "Friday · Full body", "Compound work & extra triceps", listOf(
        ExercisePlan("Goblet squat", "3 sets × 10 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=2LnkzQ7paAc"),
        ExercisePlan("Chest press", "3 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.CHEST_PRESS),
        ExercisePlan("Lat pulldown", "3 sets × 8–10 reps", ExerciseEquipment.CABLE, ExerciseVisual.LAT_PULLDOWN),
        ExercisePlan("Romanian deadlift", "3 sets × 8–10 reps", ExerciseEquipment.DUMBBELL, ExerciseVisual.DUMBBELLS, youtubeUrl = "https://www.youtube.com/watch?v=hQgFixeXdZo"),
        ExercisePlan("Shoulder press", "3 sets × 8–10 reps", ExerciseEquipment.MACHINE, ExerciseVisual.SHOULDER_PRESS),
        ExercisePlan("Triceps pushdown", "3 sets × 10–12 reps", ExerciseEquipment.CABLE, ExerciseVisual.CABLE_PUSH),
        ExercisePlan("Plank", "3 sets × 60 seconds", ExerciseEquipment.BODYWEIGHT, ExerciseVisual.PLANK, false)
    ))
)
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
    val context = LocalContext.current
    val storage = remember { AppStorage(context) }
    var profiles by remember { mutableStateOf(storage.profiles()) }
    var currentProfileId by remember { mutableStateOf(storage.currentProfileId()) }
    val profile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
    var screen by remember { mutableStateOf(if (profile == null) Screen.Welcome else Screen.Home) }
    Scaffold(containerColor = Bg, bottomBar = {
        if (screen != Screen.Welcome && screen != Screen.Voice) BottomNav(screen) { screen = it }
    }) { inset -> Box(Modifier.padding(inset).fillMaxSize().background(Bg)) {
        when (screen) {
            Screen.Welcome -> Welcome({ screen = Screen.Home }, { screen = Screen.Login })
            Screen.Login -> LoginScreen({ screen = Screen.Welcome }) { name, email ->
                val created = StoredProfile(name = name, email = email); profiles = profiles + created
                storage.saveProfiles(profiles); currentProfileId = created.id; storage.setCurrentProfile(created.id); screen = Screen.Home
            }
            Screen.Home -> HomeScreen(profile, profile?.let { storage.meals(it.id) }.orEmpty(), storage) { screen = it }
            Screen.Workouts -> WorkoutsScreen(profile, storage)
            Screen.Nutrition -> NutritionScreen(profile?.let { storage.meals(it.id) }.orEmpty()) { screen = it }
            Screen.Meals -> MealsScreen(profile, storage) { screen = Screen.Nutrition }
            Screen.Voice -> VoiceScreen({ screen = Screen.Nutrition }) { screen = Screen.Nutrition }
            Screen.Insights -> InsightsScreen()
            Screen.Progress -> ProgressScreen { screen = Screen.Health }
            Screen.Health -> HealthScreen { screen = Screen.Profile }
            Screen.Profile -> ProfileScreen(profile, profiles, { selected -> currentProfileId = selected.id; storage.setCurrentProfile(selected.id) }) { screen = it }
            Screen.EditProfile -> EditProfileScreen(profile, { screen = Screen.Profile }) { updated ->
                profiles = if (profiles.any { it.id == updated.id }) profiles.map { if (it.id == updated.id) updated else it } else profiles + updated
                storage.saveProfiles(profiles); currentProfileId = updated.id; storage.setCurrentProfile(updated.id); screen = Screen.Profile
            }
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

@Composable private fun Welcome(start: () -> Unit, login: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF111B18))) {
        Image(painterResource(R.drawable.gofrom_runner_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color(0x88000000)))
        Column(Modifier.align(Alignment.BottomCenter).padding(horizontal = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(30.dp))
            Text("GoFrom", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold); Text("Become the best version of you.", fontSize = 17.sp)
            Feature(Icons.Default.FitnessCenter, "Workouts", "Smart training plans")
            Feature(Icons.Default.Restaurant, "Nutrition", "AI food logging & insights")
            Feature(Icons.Default.ShowChart, "Progress", "Track. Improve. Succeed.")
            Button(start, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(Green)) { Text("Get Started", fontWeight = FontWeight.Bold) }
            TextButton(login, Modifier.fillMaxWidth()) { Text("Already have an account?  Log in", color = Color.White) }
            Spacer(Modifier.height(18.dp))
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

private fun greeting(profile: StoredProfile?): String {
    val base = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    return profile?.name?.takeIf { it.isNotBlank() }?.let { "$base,\n$it!" } ?: "$base!"
}

@Composable private fun HomeScreen(profile: StoredProfile?, meals: List<StoredMeal>, storage: AppStorage, go: (Screen) -> Unit) {
    var notificationSeen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val health = remember { HealthConnectManager(context) }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    var healthConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val granted = health.grantedPermissions(); healthConnected = health.hasAnyMetricPermission(granted)
        if (healthConnected) snapshot = health.sync(granted)
    }
    val todayMeals = meals.filter { it.date == LocalDate.now().toString() }
    val loggedCalories = todayMeals.mapNotNull { it.calories }
    val calories = loggedCalories.sum()
    val todayWorkout = workoutPlan.firstOrNull { it.key == LocalDate.now().dayOfWeek.name.lowercase(Locale.ROOT) }
    val workoutLogs = if (profile != null && todayWorkout != null) storage.exerciseLogs(profile.id, todayWorkout.key) else emptyList()
    val completedExercises = workoutLogs.count { it.completed }
    Page(greeting(profile), { IconButton({ notificationSeen = !notificationSeen }) { Icon(if (notificationSeen) Icons.Default.NotificationsNone else Icons.Default.Notifications, "Notifications") } }) {
    Surface(color = Color(0xFFF1F3F1), contentColor = Color(0xFF172018), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) {
        Text("Daily Overview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Ring(if (loggedCalories.isEmpty()) "—" else calories.toString(), "Kcal logged today", Green, loggedCalories.isNotEmpty())
            Ring(snapshot.stepsToday.dashboardText { it.toString() }, "Steps today", Green, snapshot.stepsToday.state == HealthDataState.AVAILABLE)
            Ring(snapshot.lastNightSleepMinutes.dashboardText { "${it / 60}h ${it % 60}m" }, "Last sleep", Blue, snapshot.lastNightSleepMinutes.state == HealthDataState.AVAILABLE)
        }
    }}
    if (!healthConnected) Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(16.dp)) {
        Text("No health data connected", fontWeight = FontWeight.Bold); Text("Connect Health Connect to show your real steps and sleep here.", color = Soft)
        TextButton({ go(Screen.Health) }) { Text("Connect Health Connect") }
    } }
    if (todayMeals.isEmpty()) Text("No meals logged today.", color = Soft)
    SectionTitle("Today's Plan", "Open workouts") { go(Screen.Workouts) }
    if (todayWorkout == null) {
        Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Panel2, shape = CircleShape) { Icon(Icons.Default.SelfImprovement, null, tint = Green, modifier = Modifier.padding(11.dp)) }
            Spacer(Modifier.width(12.dp)); Column { Text("Recovery day", fontWeight = FontWeight.Bold); Text("No workout scheduled today.", color = Soft, fontSize = 12.sp) }
        } }
    } else {
        PlanRow(
            Icons.Default.FitnessCenter,
            todayWorkout.title,
            "${todayWorkout.focus} · $completedExercises/${todayWorkout.exercises.size} completed",
            completedExercises == todayWorkout.exercises.size,
        ) { go(Screen.Workouts) }
    }
    Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Restaurant, "Log Food", Modifier.weight(1f)) { go(Screen.Meals) }; Quick(Icons.Default.FitnessCenter, "Workouts", Modifier.weight(1f)) { go(Screen.Workouts) } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Favorite, "Health", Modifier.weight(1f)) { go(Screen.Health) }; Quick(Icons.Default.ShowChart, "Progress", Modifier.weight(1f)) { go(Screen.Progress) } }
    }
}

private fun <T> HealthMetric<T>.dashboardText(format: (T) -> String): String =
    if (state == HealthDataState.AVAILABLE && value != null) format(value) else "—"

@Composable private fun Ring(value: String, label: String, color: Color, hasData: Boolean) = Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) {
        drawArc(Color(0xFFD6DCD7), -90f, 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
        if (hasData) drawArc(color, -90f, 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); Text(label, fontSize = 9.sp, textAlign = TextAlign.Center) }
}

@Composable private fun SectionTitle(title: String, link: String, click: () -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); Text(link, color = Green, modifier = Modifier.clickable(onClick = click)) }
@Composable private fun PlanRow(icon: ImageVector, title: String, sub: String, done: Boolean, click: () -> Unit) = Surface(Modifier.fillMaxWidth().clickable(onClick = click), color = Panel, shape = RoundedCornerShape(13.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Green, modifier = Modifier.size(38.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(sub, color = Soft, fontSize = 12.sp) }; Icon(if (done) Icons.Default.CheckCircle else Icons.Default.DonutLarge, null, tint = Green) } }
@Composable private fun Quick(icon: ImageVector, label: String, modifier: Modifier, click: () -> Unit) = Surface(modifier.clickable(onClick = click), color = Panel2, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = if (label == "Voice Log") Color.White else Green); Spacer(Modifier.width(9.dp)); Text(label, fontWeight = FontWeight.SemiBold) } }

@Composable private fun WorkoutsScreen(profile: StoredProfile?, storage: AppStorage) {
    val todayKey = when (LocalDate.now().dayOfWeek) {
        java.time.DayOfWeek.TUESDAY -> "tuesday"; java.time.DayOfWeek.THURSDAY -> "thursday"; java.time.DayOfWeek.FRIDAY -> "friday"; else -> "monday"
    }
    var selectedKey by remember { mutableStateOf(todayKey) }
    val selected = workoutPlan.first { it.key == selectedKey }
    Page("Workouts") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            workoutPlan.forEach { day -> FilterChip(selected = selectedKey == day.key, onClick = { selectedKey = day.key }, label = { Text(day.shortLabel) }, modifier = Modifier.weight(1f)) }
        }
        Text(selected.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(selected.focus, color = Green)
        Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(14.dp)) {
            Text("5–10 min warm-up · 60–90 sec rest", fontWeight = FontWeight.SemiBold)
            Text("Complete every work set at the top of the rep range with good form → increase the weight next time.", color = Soft, fontSize = 12.sp)
            Text("For variety, alternate equivalent machine, cable or dumbbell versions every 4–6 weeks while keeping the same movement and rep range.", color = Soft, fontSize = 12.sp)
        } }
        if (profile == null) Text("Create a profile to record weights, results and completed exercises.", color = MaterialTheme.colorScheme.error)
        selected.exercises.forEach { exercise -> ExerciseLogCard(profile, selected.key, exercise, storage) }
    }
}

@Composable private fun ExerciseLogCard(profile: StoredProfile?, dayKey: String, exercise: ExercisePlan, storage: AppStorage) {
    val context = LocalContext.current
    val saved = remember(profile?.id, dayKey, exercise.name) { profile?.let { storage.exerciseLogs(it.id, dayKey).firstOrNull { log -> log.exerciseName == exercise.name } } }
    var weight by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.weight.orEmpty()) }
    var result by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.result.orEmpty()) }
    var completed by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.completed ?: false) }
    fun persist() { profile?.let { storage.saveExerciseLog(StoredExerciseLog(it.id, dayKey, exercise.name, weight, result, completed)) } }
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ExerciseIllustration(exercise.visual, exercise.equipment, Modifier.size(width = 116.dp, height = 86.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Bold)
                Text(exercise.prescription, color = Soft, fontSize = 12.sp)
            }
            Checkbox(checked = completed, onCheckedChange = { completed = it; persist() }, enabled = profile != null)
        }
        exercise.youtubeUrl?.let { url ->
            FilledTonalButton(
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF26352B), contentColor = Green),
            ) {
                Icon(Icons.Default.PlayCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Watch dumbbell technique on YouTube", fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exercise.usesWeight) OutlinedTextField(weight, { weight = it.filter { char -> char.isDigit() || char == '.' || char == ',' }; persist() }, Modifier.weight(1f), label = { Text("Weight kg") }, singleLine = true, enabled = profile != null)
            OutlinedTextField(result, { result = it; persist() }, Modifier.weight(1f), label = { Text(if (exercise.usesWeight) "Reps per set" else "Seconds per set") }, singleLine = true, enabled = profile != null)
        }
        saved?.updatedDate?.takeIf { weight.isNotBlank() || result.isNotBlank() || completed }?.let { Text("Last saved: $it", color = Soft, fontSize = 10.sp) }
    } }
}

@Composable private fun ExerciseIllustration(
    visual: ExerciseVisual,
    equipment: ExerciseEquipment,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF0B1519))) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp)) {
            val w = size.width
            val h = size.height
            val ink = Color(0xFFDCE4E6)
            val accent = Green
            val stroke = 2.7.dp.toPx()
            fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = ink, width: Float = stroke) {
                drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), width, StrokeCap.Round)
            }
            fun box(x: Float, y: Float, width: Float, height: Float, color: Color = ink) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * x, h * y),
                    size = Size(w * width, h * height),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(stroke),
                )
            }
            fun frame() {
                line(.16f, .12f, .16f, .88f, accent)
                line(.16f, .12f, .84f, .12f, accent)
                line(.84f, .12f, .84f, .88f, accent)
                box(.10f, .70f, .12f, .18f, accent)
            }
            fun seat(x: Float = .42f, y: Float = .57f) {
                line(x, y, x + .24f, y, ink, stroke * 1.35f)
                line(x + .04f, y, x + .04f, y + .25f)
                line(x + .21f, y, x + .21f, y + .25f)
            }
            when (visual) {
                ExerciseVisual.CHEST_PRESS -> {
                    frame(); seat(); line(.42f, .34f, .42f, .60f, accent, stroke * 1.5f)
                    line(.36f, .35f, .72f, .35f); line(.36f, .29f, .36f, .48f); line(.72f, .29f, .72f, .48f)
                    line(.16f, .22f, .36f, .35f, accent)
                }
                ExerciseVisual.INCLINE_PRESS -> {
                    frame(); line(.38f, .64f, .62f, .36f, accent, stroke * 1.7f)
                    line(.35f, .67f, .70f, .67f); line(.41f, .67f, .34f, .86f); line(.66f, .67f, .73f, .86f)
                    line(.42f, .25f, .76f, .25f); line(.42f, .20f, .42f, .34f); line(.76f, .20f, .76f, .34f)
                }
                ExerciseVisual.SHOULDER_PRESS -> {
                    frame(); seat(.41f, .61f); line(.41f, .35f, .41f, .63f, accent, stroke * 1.5f)
                    line(.35f, .26f, .35f, .48f); line(.35f, .26f, .48f, .26f)
                    line(.74f, .26f, .74f, .48f); line(.61f, .26f, .74f, .26f)
                }
                ExerciseVisual.DUMBBELLS -> {
                    line(.18f, .50f, .82f, .50f, accent, stroke * 1.5f)
                    box(.13f, .31f, .10f, .38f); box(.25f, .37f, .09f, .26f)
                    box(.66f, .37f, .09f, .26f); box(.77f, .31f, .10f, .38f)
                }
                ExerciseVisual.CABLE_PUSH -> {
                    frame(); line(.16f, .20f, .62f, .35f, accent); drawCircle(ink, stroke * 1.6f, Offset(w * .62f, h * .35f))
                    line(.62f, .35f, .62f, .62f); line(.53f, .62f, .71f, .62f); line(.53f, .62f, .49f, .74f); line(.71f, .62f, .75f, .74f)
                }
                ExerciseVisual.LAT_PULLDOWN -> {
                    frame(); line(.16f, .22f, .50f, .22f, accent); line(.50f, .22f, .50f, .34f)
                    line(.28f, .34f, .72f, .34f, ink, stroke * 1.35f); seat(.39f, .66f); line(.37f, .59f, .66f, .59f, accent, stroke * 1.5f)
                }
                ExerciseVisual.SEATED_ROW -> {
                    frame(); seat(.48f, .64f); line(.16f, .32f, .43f, .57f, accent); drawCircle(ink, stroke * 1.6f, Offset(w * .43f, h * .57f))
                    line(.39f, .53f, .47f, .61f); line(.39f, .61f, .47f, .53f); line(.35f, .78f, .48f, .78f, accent, stroke * 1.5f)
                }
                ExerciseVisual.DUMBBELL_ROW -> {
                    line(.25f, .68f, .72f, .40f, accent, stroke * 1.8f); line(.31f, .68f, .22f, .86f); line(.67f, .44f, .78f, .78f)
                    line(.28f, .28f, .61f, .28f); box(.20f, .17f, .08f, .22f); box(.61f, .17f, .08f, .22f)
                }
                ExerciseVisual.PEC_DECK -> {
                    frame(); seat(); line(.42f, .33f, .42f, .60f, accent, stroke * 1.5f)
                    line(.42f, .35f, .28f, .24f); line(.28f, .24f, .28f, .54f)
                    line(.42f, .35f, .70f, .24f); line(.70f, .24f, .70f, .54f)
                }
                ExerciseVisual.LEG_PRESS -> {
                    line(.24f, .82f, .69f, .22f, accent, stroke * 1.7f); line(.66f, .16f, .82f, .30f, ink, stroke * 2f)
                    line(.23f, .63f, .43f, .79f, ink, stroke * 2f); line(.28f, .65f, .18f, .82f); line(.43f, .79f, .58f, .88f)
                    box(.71f, .17f, .10f, .18f, accent)
                }
                ExerciseVisual.LEG_CURL -> {
                    frame(); seat(.38f, .48f); line(.38f, .24f, .38f, .51f, accent, stroke * 1.5f)
                    line(.56f, .56f, .73f, .72f); drawCircle(accent, stroke * 2.7f, Offset(w * .77f, h * .75f)); line(.56f, .56f, .67f, .49f)
                }
                ExerciseVisual.LEG_EXTENSION -> {
                    frame(); seat(.38f, .48f); line(.38f, .24f, .38f, .51f, accent, stroke * 1.5f)
                    line(.58f, .56f, .58f, .78f); line(.58f, .78f, .76f, .78f); drawCircle(accent, stroke * 2.7f, Offset(w * .80f, h * .78f))
                }
                ExerciseVisual.HIP_THRUST -> {
                    line(.18f, .66f, .51f, .66f, accent, stroke * 1.8f); line(.22f, .66f, .22f, .84f); line(.47f, .66f, .47f, .84f)
                    line(.45f, .58f, .79f, .58f, ink, stroke * 1.6f); drawCircle(accent, stroke * 3f, Offset(w * .42f, h * .58f)); drawCircle(accent, stroke * 3f, Offset(w * .82f, h * .58f))
                    line(.47f, .58f, .62f, .35f); line(.62f, .35f, .78f, .58f)
                }
                ExerciseVisual.CALF_RAISE -> {
                    frame(); line(.37f, .32f, .69f, .32f, accent, stroke * 1.7f); line(.37f, .32f, .37f, .46f); line(.69f, .32f, .69f, .46f)
                    line(.51f, .45f, .51f, .75f); line(.51f, .75f, .64f, .75f); line(.42f, .83f, .70f, .83f, accent, stroke * 1.8f)
                }
                ExerciseVisual.PLANK -> {
                    drawCircle(accent, stroke * 2.8f, Offset(w * .77f, h * .34f)); line(.30f, .45f, .69f, .39f, ink, stroke * 1.7f)
                    line(.30f, .45f, .17f, .70f); line(.17f, .70f, .31f, .70f); line(.69f, .39f, .61f, .66f); line(.61f, .66f, .79f, .66f)
                    line(.12f, .78f, .88f, .78f, accent)
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
            color = Color(0xDD182327),
            shape = RoundedCornerShape(6.dp),
        ) { Text(equipment.label, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
    }
}

@Composable private fun NutritionScreen(meals: List<StoredMeal>, go: (Screen) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }
    Page("Nutrition", { IconButton({ date = LocalDate.now() }) { Icon(Icons.Default.CalendarMonth, "Today") } }) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton({ date = date.minusDays(1) }) { Icon(Icons.Default.ChevronLeft, "Previous day") }; Text(if (date == LocalDate.now()) "Today, ${date.format(formatter)}" else date.format(formatter), fontWeight = FontWeight.Bold); IconButton({ date = date.plusDays(1) }) { Icon(Icons.Default.ChevronRight, "Next day") } } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { Text("Log", color = Green); Text("Meals", modifier = Modifier.clickable { go(Screen.Meals) }); Text("Insights", modifier = Modifier.clickable { go(Screen.Insights) }) }
    val selected = meals.filter { it.date == date.toString() }
    Text("Calories", fontWeight = FontWeight.Bold)
    Text(if (selected.isEmpty()) "No calories logged" else "${selected.mapNotNull { it.calories }.sum()} kcal logged", color = Soft)
    if (selected.isEmpty()) Text("No meals added for this day.", color = Soft)
    selected.forEach { meal -> MealRow(meal) }
    Button({ go(Screen.Meals) }, Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(Green)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add meal") }
    }
}

@Composable private fun MealsScreen(profile: StoredProfile?, storage: AppStorage, back: () -> Unit) {
    var mealName by remember { mutableStateOf("") }; var calories by remember { mutableStateOf("") }; var photoUri by remember { mutableStateOf<String?>(null) }
    Page("Meals", { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) {
        if (profile == null) Text("Create a profile first so this meal can be saved.", color = Soft)
        PhotoChooser(photoUri, "Add meal photo") { photoUri = it }
        OutlinedTextField(mealName, { mealName = it }, Modifier.fillMaxWidth(), label = { Text("Meal name") })
        OutlinedTextField(calories, { calories = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Calories") })
        Button({ profile?.let { storage.addMeal(StoredMeal(profileId = it.id, name = mealName.trim(), calories = calories.toIntOrNull(), photoUri = photoUri)); back() } }, Modifier.fillMaxWidth(), enabled = profile != null && mealName.isNotBlank(), colors = ButtonDefaults.buttonColors(Green)) { Text("Save meal") }
    }
}

@Composable private fun FoodRow(meal: String, food: String) { var edit by remember { mutableStateOf(false) }; var value by remember { mutableStateOf(food) }; Column { Text(meal, fontWeight = FontWeight.Bold); Surface(Modifier.fillMaxWidth().clickable { edit = true }, color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Restaurant, null, tint = Green); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(value); Text("Tap to edit", color = Soft, fontSize = 11.sp) }; Icon(Icons.Default.Add, null, tint = Green) } }; if (edit) OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), trailingIcon = { IconButton({ edit = false }) { Icon(Icons.Default.Check, null) } }) } }

@Composable private fun MealRow(meal: StoredMeal) = Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        SavedPhoto(meal.photoUri, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(meal.name, fontWeight = FontWeight.Bold); meal.calories?.let { Text("$it kcal", color = Green) } }
    }
}

@Composable private fun PhotoChooser(uri: String?, label: String, selected: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked: Uri? -> picked?.let {
        runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        selected(it.toString())
    } }
    Surface(Modifier.fillMaxWidth().clickable { launcher.launch(arrayOf("image/*")) }, color = Panel, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SavedPhoto(uri, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(14.dp)); Text(if (uri == null) label else "Change photo", color = Green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun SavedPhoto(uri: String?, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) { uri?.let { value -> runCatching { context.contentResolver.openInputStream(Uri.parse(value))?.use(BitmapFactory::decodeStream) }.getOrNull() } }
    if (bitmap != null) Image(bitmap.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
    else Surface(modifier, color = Panel2) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, tint = Soft) } }
}

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
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(18.dp)) {
        Text("No insights yet", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Insights will be calculated after you have logged enough meals.", color = Soft)
    } }
}

@Composable private fun BarChart() = Row(Modifier.fillMaxWidth().height(145.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) { listOf(.55f,.7f,.86f,.72f,.74f,.73f,.62f).forEachIndexed { i, h -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.width(14.dp).fillMaxHeight(h).background(Green, RoundedCornerShape(3.dp))); Text(listOf("M","T","W","T","F","S","S")[i], fontSize = 10.sp, color = Soft) } } }

private enum class YearMetricView(val label: String, val shortLabel: String) {
    STEPS("Steps", "Steps"),
    ACTIVE_CALORIES("Active calories", "Calories"),
    SLEEP("Recorded sleep", "Sleep"),
    HEART_RATE("Heart rate", "Heart"),
    WEIGHT("Weight", "Weight")
}

private enum class YearChartStyle(val label: String) { TREND("Smooth trend"), HYBRID("Bars + trend") }

@Composable private fun ProgressScreen(openHealth: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()
    var year by remember { mutableStateOf(HealthYearSnapshot()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun refresh() {
        loading = true
        val granted = manager.grantedPermissions()
        year = manager.syncYear(granted)
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }
    Page("Progress", {
        IconButton({ scope.launch { refresh() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "Refresh health history") }
    }) {
        Text("Health history", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Your last 12 calendar months from Health Connect. No sample values are added.", color = Soft, fontSize = 12.sp)
        when {
            loading -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
            !year.historySupported -> HealthYearMessage(
                icon = Icons.Default.SystemUpdate,
                title = "Past data is unavailable",
                text = "Update Android and Health Connect to enable access to data older than 30 days.",
                button = "Open Health Connect",
                click = openHealth
            )
            !year.historyAccessGranted -> HealthYearMessage(
                icon = Icons.Default.History,
                title = "Past data permission required",
                text = "Allow ‘Access past data’ in Health Connect to build your real 12-month overview.",
                button = "Manage Health Connect access",
                click = openHealth
            )
            year.hasYearData() -> HealthYearOverview(year)
            else -> HealthYearMessage(
                icon = Icons.Default.QueryStats,
                title = "No historical records found",
                text = "Health Connect did not return steps, active calories, sleep, heart-rate or weight data for this period.",
                button = "Check Health Connect",
                click = openHealth
            )
        }
        year.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun HealthYearSnapshot.hasYearData(): Boolean = listOf(
    totalSteps.state,
    totalActiveCalories.state,
    recordedSleepMinutes.state,
    averageHeartRate.state,
    averageWeightKg.state
).any { it == HealthDataState.AVAILABLE }

@Composable private fun HealthYearMessage(icon: ImageVector, title: String, text: String, button: String, click: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(34.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text, color = Soft)
        Button(click, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text(button) }
    } }
}

@Composable private fun HealthYearOverview(year: HealthYearSnapshot) {
    val availableMetrics = YearMetricView.entries.filter { metric -> year.months.any { it.valueFor(metric) != null } }
    var selected by remember(year.lastSynced) { mutableStateOf(availableMetrics.firstOrNull() ?: YearMetricView.STEPS) }
    var chartStyle by remember { mutableStateOf(YearChartStyle.TREND) }
    val values = year.months.map { it.valueFor(selected) }
    val recorded = values.mapIndexedNotNull { index, value -> value?.let { index to it } }
    val best = recorded.maxByOrNull { it.second }
    val average = recorded.map { it.second }.average().takeUnless { it.isNaN() }

    Text("Google Health synced", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text("Your health story, based only on records available in Health Connect.", color = Soft, fontSize = 12.sp)

    val heroShape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(heroShape)
            .background(Brush.linearGradient(listOf(Color(0xFF18272C), Color(0xFF0D171A))))
            .border(1.dp, Color(0xFF263A40), heroShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF21351B), shape = RoundedCornerShape(14.dp)) {
                Icon(selected.icon(), null, tint = Green, modifier = Modifier.padding(11.dp).size(24.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(selected.label, color = Soft, fontSize = 12.sp)
                Text(year.summaryFor(selected), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            DataCoverageRing(recorded.size, year.months.size)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            YearFact(
                "Average",
                average?.let { selected.formatValue(it) } ?: "No data",
                Modifier.weight(1f)
            )
            YearFact(
                "Best month",
                best?.let { (index, value) ->
                    "${year.months[index].month.format(DateTimeFormatter.ofPattern("MMM"))} · ${selected.formatValue(value)}"
                } ?: "No data",
                Modifier.weight(1f)
            )
            YearFact("Source", "Health Connect", Modifier.weight(1f))
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Bg.copy(alpha = .68f)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            YearChartStyle.entries.forEach { style ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (chartStyle == style) Color(0xFF21351B) else Color.Transparent)
                        .clickable { chartStyle = style }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(style.label, color = if (chartStyle == style) Green else Soft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HealthYearTrendChart(year.months, selected, chartStyle)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 4, 8, 11).forEach { index ->
                Text(year.months[index].month.format(DateTimeFormatter.ofPattern("MMM")), color = Soft, fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(18.dp).height(2.dp).background(Soft.copy(alpha = .55f)))
            Spacer(Modifier.width(7.dp))
            Text("Missing month = no Health Connect record", color = Soft, fontSize = 11.sp)
        }
    }

    Text("Explore your health", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        YearMetricView.entries.forEach { metric ->
            val hasData = year.months.any { it.valueFor(metric) != null }
            val chosen = selected == metric
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (chosen) Color(0xFF1D3018) else Panel)
                    .border(1.dp, if (chosen) Green.copy(alpha = .75f) else Color(0xFF29373C), RoundedCornerShape(14.dp))
                    .clickable(enabled = hasData) { selected = metric }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(metric.icon(), null, tint = if (chosen) Green else Soft, modifier = Modifier.size(19.dp))
                Spacer(Modifier.height(4.dp))
                Text(metric.shortLabel, color = if (hasData) if (chosen) Green else Soft else Soft.copy(alpha = .35f), fontSize = 9.sp)
            }
        }
    }
}

@Composable private fun DataCoverageRing(recordedMonths: Int, totalMonths: Int) {
    Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val line = 6.dp.toPx()
            drawArc(Soft.copy(alpha = .2f), -90f, 360f, false, style = Stroke(line, cap = StrokeCap.Round))
            if (totalMonths > 0 && recordedMonths > 0) {
                drawArc(Green, -90f, 360f * recordedMonths / totalMonths, false, style = Stroke(line, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$recordedMonths/$totalMonths", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("months", color = Soft, fontSize = 8.sp)
        }
    }
}

@Composable private fun YearFact(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(Bg.copy(alpha = .42f)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = Soft, fontSize = 9.sp)
        Text(value, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun HealthYearTrendChart(months: List<HealthMonthPoint>, metric: YearMetricView, style: YearChartStyle) {
    val values = months.map { it.valueFor(metric) }
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val present = values.mapIndexedNotNull { index, value -> value?.let { index to it } }
        if (present.isEmpty()) return@Canvas

        val minimum = present.minOf { it.second }
        val maximum = present.maxOf { it.second }
        val span = maxOf(maximum - minimum, maximum * .08, 1.0)
        val lower = if (metric == YearMetricView.HEART_RATE || metric == YearMetricView.WEIGHT) minimum - span * .32 else 0.0
        val upper = maximum + span * .12
        val top = 9.dp.toPx()
        val baseline = size.height - 4.dp.toPx()
        val plotHeight = baseline - top
        val slot = size.width / values.size.coerceAtLeast(1)
        fun point(index: Int, value: Double) = Offset(
            x = slot * (index + .5f),
            y = top + ((upper - value) / (upper - lower)).toFloat() * plotHeight
        )
        fun smoothPath(points: List<Offset>): Path = Path().apply {
            if (points.isEmpty()) return@apply
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEachIndexed { index, current ->
                val previous = points[index]
                val middle = (previous.x + current.x) / 2f
                cubicTo(middle, previous.y, middle, current.y, current.x, current.y)
            }
        }

        listOf(0f, .5f, 1f).forEach { position ->
            val y = top + plotHeight * position
            drawLine(
                Soft.copy(alpha = if (position == 1f) .27f else .14f),
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
            )
        }

        if (style == YearChartStyle.HYBRID) {
            val barWidth = (slot * .52f).coerceIn(10.dp.toPx(), 18.dp.toPx())
            present.forEach { (index, value) ->
                val graphPoint = point(index, value)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF9BE969), Color(0xFF4F9C2B)), startY = graphPoint.y, endY = baseline),
                    topLeft = Offset(graphPoint.x - barWidth / 2f, graphPoint.y),
                    size = Size(barWidth, baseline - graphPoint.y),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    alpha = .72f
                )
            }
        }

        val segments = mutableListOf<List<Offset>>()
        var segment = mutableListOf<Offset>()
        values.forEachIndexed { index, value ->
            if (value == null) {
                if (segment.isNotEmpty()) segments += segment.toList()
                segment = mutableListOf()
            } else segment += point(index, value)
        }
        if (segment.isNotEmpty()) segments += segment.toList()

        segments.forEach { points ->
            if (style == YearChartStyle.TREND && points.size > 1) {
                val area = smoothPath(points).apply {
                    lineTo(points.last().x, baseline)
                    lineTo(points.first().x, baseline)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(Green.copy(alpha = .38f), Green.copy(alpha = .01f)), top, baseline))
            }
            if (points.size > 1) {
                val path = smoothPath(points)
                drawPath(path, Green.copy(alpha = .18f), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color(0xFF9BE969), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            points.forEach { graphPoint ->
                drawCircle(Color(0xFF9BE969), 5.dp.toPx(), graphPoint)
                drawCircle(Bg, 2.8.dp.toPx(), graphPoint)
            }
        }

        values.forEachIndexed { index, value ->
            if (value == null) {
                val center = slot * (index + .5f)
                drawLine(
                    Soft.copy(alpha = .55f),
                    Offset(center - 7.dp.toPx(), baseline - 2.dp.toPx()),
                    Offset(center + 7.dp.toPx(), baseline - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                )
            }
        }
    }
}

private fun HealthMonthPoint.valueFor(metric: YearMetricView): Double? = when (metric) {
    YearMetricView.STEPS -> steps?.toDouble()
    YearMetricView.ACTIVE_CALORIES -> activeCalories?.toDouble()
    YearMetricView.SLEEP -> sleepMinutes?.div(60.0)
    YearMetricView.HEART_RATE -> averageHeartRate?.toDouble()
    YearMetricView.WEIGHT -> averageWeightKg
}

private fun YearMetricView.icon(): ImageVector = when (this) {
    YearMetricView.STEPS -> Icons.Default.DirectionsWalk
    YearMetricView.ACTIVE_CALORIES -> Icons.Default.LocalFireDepartment
    YearMetricView.SLEEP -> Icons.Default.Bedtime
    YearMetricView.HEART_RATE -> Icons.Default.FavoriteBorder
    YearMetricView.WEIGHT -> Icons.Default.MonitorWeight
}

private fun YearMetricView.formatValue(value: Double): String = when (this) {
    YearMetricView.STEPS -> formatWholeNumber(value.roundToLong())
    YearMetricView.ACTIVE_CALORIES -> "${formatWholeNumber(value.roundToLong())} kcal"
    YearMetricView.SLEEP -> "%.0f h".format(value)
    YearMetricView.HEART_RATE -> "%.0f bpm".format(value)
    YearMetricView.WEIGHT -> "%.1f kg".format(value)
}

private fun HealthYearSnapshot.summaryFor(metric: YearMetricView): String = when (metric) {
    YearMetricView.STEPS -> totalSteps.healthValue { formatWholeNumber(it) }
    YearMetricView.ACTIVE_CALORIES -> totalActiveCalories.healthValue { "${formatWholeNumber(it.toLong())} kcal" }
    YearMetricView.SLEEP -> recordedSleepMinutes.healthValue { "${formatWholeNumber(it / 60)} h" }
    YearMetricView.HEART_RATE -> averageHeartRate.healthValue { "$it bpm avg" }
    YearMetricView.WEIGHT -> averageWeightKg.healthValue { "%.1f kg avg".format(it) }
}

private fun formatWholeNumber(value: Long): String = String.format(Locale.getDefault(), "%,d", value)
@Composable private fun MetricCard(title: String, value: String, change: String) = Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(title, color = Soft); Text(value, fontWeight = FontWeight.Bold) }; Text(change, color = Green) } }

@Composable private fun HealthScreen(back: () -> Unit) {
    val context = LocalContext.current; val manager = remember { HealthConnectManager(context) }; val scope = rememberCoroutineScope(); var granted by remember { mutableStateOf<Set<String>>(emptySet()) }; var data by remember { mutableStateOf(HealthSnapshot()) }; var connectionError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(manager.permissionContract()) {
        scope.launch {
            granted = manager.grantedPermissions()
            connectionError = if (!manager.hasAnyMetricPermission(granted)) "No access was granted. You can try again and select the data you want to share." else null
            data = manager.sync(granted)
        }
    }
    LaunchedEffect(Unit) {
        granted = manager.grantedPermissions()
        data = manager.sync(granted)
    }
    Page("Health Connect", { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) {
        Surface(Modifier.align(Alignment.CenterHorizontally).size(104.dp), color = Color(0xFF183621), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.HealthAndSafety, null, tint = Green, modifier = Modifier.size(62.dp)) }
        }
        Text(when {
            manager.availability() != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE -> "Health Connect unavailable"
            manager.hasAllMetricPermissions(granted) -> "Connected"
            manager.hasAnyMetricPermission(granted) -> "Partly connected"
            else -> "Permission required"
        }, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        data.lastSynced?.let { Text("Last synced: ${DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(it)}", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Soft) }
        Text("Synced data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Activity and sleep totals follow the source priorities you set in Health Connect.", color = Soft, fontSize = 12.sp)
        HealthMetricRow(Icons.Default.DirectionsWalk, "Steps", "Today", data.stepsToday) { it.toString() }
        HealthMetricRow(Icons.Default.FavoriteBorder, "Heart rate", "Latest measurement in the past 24 hours", data.latestHeartRate) { "$it bpm" }
        HealthMetricRow(Icons.Default.Bedtime, "Sleep", "Last night · 18:00–12:00", data.lastNightSleepMinutes) { "${it / 60}h ${it % 60}m" }
        HealthMetricRow(Icons.Default.LocalFireDepartment, "Active calories", "Burned today", data.activeCaloriesToday) { "$it kcal" }
        HealthMetricRow(Icons.Default.MonitorWeight, "Weight", "Latest measurement in the past 30 days", data.latestWeightKg) { "%.1f kg".format(it) }
        HistoryAccessRow(manager.supportsHistoryRead(), manager.hasHistoryPermission(granted))
        (connectionError ?: data.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button({
            connectionError = null
            if (!manager.hasAllMetricPermissions(granted)) {
                runCatching { permissionLauncher.launch(manager.permissions) }
                    .onFailure { connectionError = "Health Connect could not be opened: ${it.message ?: "unknown error"}" }
            } else scope.launch { data = manager.sync(granted) }
        }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), enabled = manager.availability() == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE, colors = ButtonDefaults.buttonColors(Green)) { Text(if (manager.hasAllMetricPermissions(granted)) "Sync Now" else "Manage Health Connect access", fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun HistoryAccessRow(supported: Boolean, granted: Boolean) = Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Panel2, shape = CircleShape) { Icon(Icons.Default.History, null, tint = Green, modifier = Modifier.padding(10.dp).size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Past 12 months", fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !supported -> "Update Health Connect to enable history"
                    granted -> "Past data access granted"
                    else -> "Allow access to data older than 30 days"
                },
                color = Soft,
                fontSize = 11.sp
            )
        }
        Icon(
            when {
                !supported -> Icons.Default.Error
                granted -> Icons.Default.CheckCircle
                else -> Icons.Default.Lock
            },
            null,
            tint = if (granted) Green else Soft,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable private fun <T> HealthMetricRow(
    icon: ImageVector,
    title: String,
    period: String,
    metric: HealthMetric<T>,
    format: (T) -> String
) = Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Panel2, shape = CircleShape) { Icon(icon, null, tint = Green, modifier = Modifier.padding(10.dp).size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(metric.healthDetail(period), color = Soft, fontSize = 11.sp)
        }
        Text(metric.healthValue(format), color = if (metric.state == HealthDataState.AVAILABLE) Green else Soft, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(
            when (metric.state) {
                HealthDataState.AVAILABLE -> Icons.Default.CheckCircle
                HealthDataState.ERROR -> Icons.Default.Error
                HealthDataState.PERMISSION_REQUIRED -> Icons.Default.Lock
                HealthDataState.NO_DATA -> Icons.Default.RemoveCircleOutline
            },
            null,
            tint = if (metric.state == HealthDataState.AVAILABLE) Green else Soft,
            modifier = Modifier.size(19.dp)
        )
    }
}

private fun <T> HealthMetric<T>.healthValue(format: (T) -> String): String = when {
    state == HealthDataState.AVAILABLE && value != null -> format(value)
    state == HealthDataState.PERMISSION_REQUIRED -> "No access"
    state == HealthDataState.ERROR -> "Unavailable"
    else -> "No data"
}

private fun HealthMetric<*>.healthDetail(period: String): String = when (state) {
    HealthDataState.PERMISSION_REQUIRED -> "Permission not granted"
    HealthDataState.ERROR -> error ?: "Could not be read"
    else -> measuredAt?.let { instant ->
        "$period · ${DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault()).format(instant)}"
    } ?: period
}

@Composable private fun ProfileScreen(profile: StoredProfile?, profiles: List<StoredProfile>, select: (StoredProfile) -> Unit, go: (Screen) -> Unit) = Page("Profile", { IconButton({ go(Screen.EditProfile) }) { Icon(Icons.Default.Settings, "Edit profile") } }) {
    SavedPhoto(profile?.photoUri, Modifier.align(Alignment.CenterHorizontally).size(92.dp).clip(CircleShape))
    Text(profile?.name ?: "No profile yet", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    Text(profile?.email ?: "Create your first profile", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Soft)
    if (profiles.size > 1) {
        Text("Switch profile", fontWeight = FontWeight.Bold)
        profiles.forEach { item -> Surface(Modifier.fillMaxWidth().clickable { select(item) }, color = if (item.id == profile?.id) Panel2 else Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { SavedPhoto(item.photoUri, Modifier.size(42.dp).clip(CircleShape)); Spacer(Modifier.width(12.dp)); Text(item.name, Modifier.weight(1f)); if (item.id == profile?.id) Icon(Icons.Default.Check, null, tint = Green) } } }
    }
    Button({ go(Screen.EditProfile) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Panel2)) { Text(if (profile == null) "Create profile" else "Edit profile") }
    Button({ go(Screen.Login) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Panel2)) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Add another user") }
    Button({ go(Screen.Health) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text("Health Connect") }
}

@Composable private fun LoginScreen(back: () -> Unit, save: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }
    Page("Log in or create profile", { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) {
        Text("Your name is only shown after you create a profile.", color = Soft)
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Button({ save(name.trim(), email.trim()) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && email.contains("@"), colors = ButtonDefaults.buttonColors(Green)) { Text("Continue") }
    }
}

@Composable private fun EditProfileScreen(profile: StoredProfile?, back: () -> Unit, save: (StoredProfile) -> Unit) {
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }; var email by remember(profile) { mutableStateOf(profile?.email.orEmpty()) }; var photoUri by remember(profile) { mutableStateOf(profile?.photoUri) }
    Page("Profile settings", { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") } }) {
        PhotoChooser(photoUri, "Add profile photo") { photoUri = it }
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Button({ save(StoredProfile(id = profile?.id ?: java.util.UUID.randomUUID().toString(), name = name.trim(), email = email.trim(), photoUri = photoUri)) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && email.contains("@"), colors = ButtonDefaults.buttonColors(Green)) { Text("Save profile") }
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier) = Surface(modifier, color = Panel, shape = RoundedCornerShape(12.dp)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Soft, fontSize = 11.sp); Text(value, fontWeight = FontWeight.Bold) } }
