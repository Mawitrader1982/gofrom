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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private val Green = Color(0xFF83D445)
private val Bg = Color(0xFF071014)
private val Panel = Color(0xFF121C20)
private val Panel2 = Color(0xFF182327)
private val Soft = Color(0xFFA9B2B5)
private val Purple = Color(0xFFB88BDD)
private val Blue = Color(0xFF65B9E8)

private enum class Screen { Welcome, Login, Home, Workouts, Nutrition, Meals, Voice, Insights, Progress, Health, Profile, EditProfile }
private data class ExercisePlan(val name: String, val prescription: String, val usesWeight: Boolean = true)
private data class WorkoutDay(val key: String, val shortLabel: String, val title: String, val focus: String, val exercises: List<ExercisePlan>)

private val workoutPlan = listOf(
    WorkoutDay("monday", "Mon", "Monday · Push", "Chest, shoulders & triceps", listOf(
        ExercisePlan("Chest press", "3 sets × 8–10 reps"), ExercisePlan("Incline chest press", "3 sets × 8–10 reps"),
        ExercisePlan("Shoulder press", "3 sets × 8–10 reps"), ExercisePlan("Lateral raise", "3 sets × 12–15 reps"),
        ExercisePlan("Triceps pushdown", "3 sets × 10–12 reps"), ExercisePlan("Overhead triceps extension", "3 sets × 10–12 reps"),
        ExercisePlan("Plank", "3 sets × 60 seconds", false)
    )),
    WorkoutDay("tuesday", "Tue", "Tuesday · Pull", "Back & biceps", listOf(
        ExercisePlan("Lat pulldown", "3 sets × 8–10 reps"), ExercisePlan("Seated cable row", "3 sets × 8–10 reps"),
        ExercisePlan("Chest-supported row", "3 sets × 8–10 reps"), ExercisePlan("Reverse fly", "3 sets × 12–15 reps"),
        ExercisePlan("Biceps curl", "3 sets × 10–12 reps"), ExercisePlan("Hammer curl", "3 sets × 10–12 reps"),
        ExercisePlan("Plank", "3 sets × 60 seconds", false)
    )),
    WorkoutDay("thursday", "Thu", "Thursday · Legs", "Legs & glutes", listOf(
        ExercisePlan("Leg press", "4 sets × 8–10 reps"), ExercisePlan("Leg curl", "3 sets × 10–12 reps"),
        ExercisePlan("Leg extension", "3 sets × 10–12 reps"), ExercisePlan("Hip thrust", "3 sets × 8–12 reps"),
        ExercisePlan("Calf raise", "3 sets × 12–15 reps"), ExercisePlan("Plank", "3 sets × 60 seconds", false)
    )),
    WorkoutDay("friday", "Fri", "Friday · Full body", "Compound work & extra triceps", listOf(
        ExercisePlan("Goblet squat", "3 sets × 10 reps"), ExercisePlan("Chest press", "3 sets × 8–10 reps"),
        ExercisePlan("Lat pulldown", "3 sets × 8–10 reps"), ExercisePlan("Romanian deadlift", "3 sets × 8–10 reps"),
        ExercisePlan("Shoulder press", "3 sets × 8–10 reps"), ExercisePlan("Triceps pushdown", "3 sets × 10–12 reps"),
        ExercisePlan("Plank", "3 sets × 60 seconds", false)
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
            Screen.Home -> HomeScreen(profile, profile?.let { storage.meals(it.id) }.orEmpty()) { screen = it }
            Screen.Workouts -> WorkoutsScreen(profile, storage)
            Screen.Nutrition -> NutritionScreen(profile?.let { storage.meals(it.id) }.orEmpty()) { screen = it }
            Screen.Meals -> MealsScreen(profile, storage) { screen = Screen.Nutrition }
            Screen.Voice -> VoiceScreen({ screen = Screen.Nutrition }) { screen = Screen.Nutrition }
            Screen.Insights -> InsightsScreen()
            Screen.Progress -> ProgressScreen()
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

@Composable private fun HomeScreen(profile: StoredProfile?, meals: List<StoredMeal>, go: (Screen) -> Unit) {
    var notificationSeen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val health = remember { HealthConnectManager(context) }
    var snapshot by remember { mutableStateOf(HealthSnapshot()) }
    var healthConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val granted = health.grantedPermissions(); healthConnected = granted.isNotEmpty()
        if (healthConnected) snapshot = health.sync(granted)
    }
    val todayMeals = meals.filter { it.date == LocalDate.now().toString() }
    val calories = todayMeals.mapNotNull { it.calories }.sum()
    Page(greeting(profile), { IconButton({ notificationSeen = !notificationSeen }) { Icon(if (notificationSeen) Icons.Default.NotificationsNone else Icons.Default.Notifications, "Notifications") } }) {
    Surface(color = Color(0xFFF1F3F1), contentColor = Color(0xFF172018), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) {
        Text("Daily Overview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Ring(if (todayMeals.isEmpty()) "—" else calories.toString(), "Logged kcal", 0f, Green)
            Ring(if (healthConnected) snapshot.steps.toString() else "—", "Health steps", 0f, Green)
            Ring(if (healthConnected) "${snapshot.sleepMinutes / 60}h" else "—", "Health sleep", 0f, Blue)
        }
    }}
    if (!healthConnected) Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(16.dp)) {
        Text("No health data connected", fontWeight = FontWeight.Bold); Text("Connect Health Connect to show your real steps and sleep here.", color = Soft)
        TextButton({ go(Screen.Health) }) { Text("Connect Health Connect") }
    } }
    if (todayMeals.isEmpty()) Text("No meals logged today.", color = Soft)
    Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Restaurant, "Log Food", Modifier.weight(1f)) { go(Screen.Meals) }; Quick(Icons.Default.FitnessCenter, "Workouts", Modifier.weight(1f)) { go(Screen.Workouts) } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Quick(Icons.Default.Favorite, "Health", Modifier.weight(1f)) { go(Screen.Health) }; Quick(Icons.Default.ShowChart, "Progress", Modifier.weight(1f)) { go(Screen.Progress) } }
    }
}

@Composable private fun Ring(value: String, label: String, progress: Float, color: Color) = Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
    Canvas(Modifier.fillMaxSize()) { drawArc(Color(0xFFD6DCD7), -90f, 360f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)); drawArc(color, -90f, progress * 360, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)) }
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
    val saved = remember(profile?.id, dayKey, exercise.name) { profile?.let { storage.exerciseLogs(it.id, dayKey).firstOrNull { log -> log.exerciseName == exercise.name } } }
    var weight by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.weight.orEmpty()) }
    var result by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.result.orEmpty()) }
    var completed by remember(profile?.id, dayKey, exercise.name) { mutableStateOf(saved?.completed ?: false) }
    fun persist() { profile?.let { storage.saveExerciseLog(StoredExerciseLog(it.id, dayKey, exercise.name, weight, result, completed)) } }
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(exercise.name, fontWeight = FontWeight.Bold); Text(exercise.prescription, color = Soft, fontSize = 12.sp) }
            Checkbox(checked = completed, onCheckedChange = { completed = it; persist() }, enabled = profile != null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exercise.usesWeight) OutlinedTextField(weight, { weight = it.filter { char -> char.isDigit() || char == '.' || char == ',' }; persist() }, Modifier.weight(1f), label = { Text("Weight kg") }, singleLine = true, enabled = profile != null)
            OutlinedTextField(result, { result = it; persist() }, Modifier.weight(1f), label = { Text(if (exercise.usesWeight) "Reps per set" else "Seconds per set") }, singleLine = true, enabled = profile != null)
        }
        saved?.updatedDate?.takeIf { weight.isNotBlank() || result.isNotBlank() || completed }?.let { Text("Last saved: $it", color = Soft, fontSize = 10.sp) }
    } }
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

@Composable private fun ProgressScreen() = Page("Progress") {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(18.dp)) {
        Text("No progress data yet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Real Health Connect and workout history will appear here once available.", color = Soft)
    } }
}
@Composable private fun MetricCard(title: String, value: String, change: String) = Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(title, color = Soft); Text(value, fontWeight = FontWeight.Bold) }; Text(change, color = Green) } }

@Composable private fun HealthScreen(back: () -> Unit) {
    val context = LocalContext.current; val manager = remember { HealthConnectManager(context) }; val scope = rememberCoroutineScope(); var granted by remember { mutableStateOf<Set<String>>(emptySet()) }; var data by remember { mutableStateOf(HealthSnapshot()) }; var connectionError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(manager.permissionContract()) { result ->
        granted = result
        connectionError = if (result.isEmpty()) "No access was granted. You can try again and select the data you want to share." else null
        if (result.isNotEmpty()) scope.launch { data = manager.sync(result) }
    }
    LaunchedEffect(Unit) { granted = manager.grantedPermissions(); if (granted.isNotEmpty()) data = manager.sync(granted) }
    Page("Google Health", { IconButton(back) { Icon(Icons.Default.ArrowBack, null) } }) {
        Icon(Icons.Default.Favorite, null, tint = Green, modifier = Modifier.align(Alignment.CenterHorizontally).size(95.dp))
        Text(when { manager.availability() != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE -> "Health Connect unavailable"; granted.containsAll(manager.permissions) -> "Connected"; granted.isNotEmpty() -> "Partly connected"; else -> "Permission required" }, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 20.sp)
        data.lastSynced?.let { Text("Last synced: ${DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(it)}", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Soft) }
        val hasAccess = granted.isNotEmpty()
        listOf(
            "Steps" to if (hasAccess) data.steps.toString() else "—",
            "Heart Rate" to if (hasAccess) (data.heartRate?.let { "$it bpm" } ?: "—") else "—",
            "Sleep" to if (hasAccess) "${data.sleepMinutes / 60}h ${data.sleepMinutes % 60}m" else "—",
            "Calories" to if (hasAccess) "${data.calories} kcal" else "—",
            "Weight" to if (hasAccess) (data.weightKg?.let { "%.1f kg".format(it) } ?: "—") else "—"
        ).forEach { (label, value) -> Surface(color = Panel, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, color = Green) } } }
        (connectionError ?: data.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button({
            connectionError = null
            if (!granted.containsAll(manager.permissions)) {
                runCatching { permissionLauncher.launch(manager.permissions) }
                    .onFailure { connectionError = "Health Connect could not be opened: ${it.message ?: "unknown error"}" }
            } else scope.launch { data = manager.sync(granted) }
        }, Modifier.fillMaxWidth(), enabled = manager.availability() == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE, colors = ButtonDefaults.buttonColors(Green)) { Text(if (granted.containsAll(manager.permissions)) "Sync Now" else "Connect Health Connect") }
    }
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
    Button({ go(Screen.Health) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Green)) { Text("Google Health Sync") }
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
