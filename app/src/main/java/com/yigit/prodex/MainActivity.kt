package com.yigit.prodex

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.yigit.prodex.data.Expense
import com.yigit.prodex.data.ExpenseCategory
import com.yigit.prodex.data.Task
import com.yigit.prodex.data.User
import com.yigit.prodex.ui.events.EventFinderScreen // NEW: Event Finder Imported
import com.yigit.prodex.ui.expenses.ExpenseViewModel
import com.yigit.prodex.ui.expenses.ExpenseViewModelFactory
import com.yigit.prodex.ui.game.BerlinRunnerScreen // NEW: Game Screen Alias
import com.yigit.prodex.ui.game.GameCharacter // NEW: Character Enum
import com.yigit.prodex.ui.profile.ProfileViewModel
import com.yigit.prodex.ui.profile.ProfileViewModelFactory
import com.yigit.prodex.ui.tasks.TaskViewModel
import com.yigit.prodex.ui.tasks.TaskViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- THEME & TEXT CONSTANTS ---
val LightBackgroundColor = Color(0xFFFFFFFF)
val AccentColor = Color(0xFF448AFF)
val CardColor = Color(0xFFF5F5F5)
val TextColor = Color(0xFF000000)

// --- UTILITY FUNCTIONS ---
fun formatDate(timeInMillis: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date(timeInMillis))
}

fun isValidPassword(password: String): Boolean {
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}

fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    return email.matches(emailRegex.toRegex())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as ProdExApplication
            ProdExApp(
                taskViewModel = viewModel(factory = TaskViewModelFactory(application.database.taskDao())),
                expenseViewModel = viewModel(factory = ExpenseViewModelFactory(application.database.expenseDao())),
                profileViewModel = viewModel(factory = ProfileViewModelFactory(application.database.userDao(), application.sessionManager))
            )
        }
    }
}

@Composable
fun ProdExApp(taskViewModel: TaskViewModel, expenseViewModel: ExpenseViewModel, profileViewModel: ProfileViewModel) {
    val activeUserId by profileViewModel.activeUserId.collectAsState()

    if (activeUserId == null) {
        AuthNavigation(profileViewModel)
    } else {
        MainAppScaffold(taskViewModel, expenseViewModel, profileViewModel)
    }

    LaunchedEffect(activeUserId) {
        if (activeUserId != null) {
            val tasks = taskViewModel.getTasksForUser(activeUserId!!).first()
            if (tasks.isEmpty()) {
                taskViewModel.addTask(activeUserId!!, "Read a Book 📚", null)
                taskViewModel.addTask(activeUserId!!, "Clean the House 🧹", null)
                taskViewModel.addTask(activeUserId!!, "Go Shopping 🛒", null)
                taskViewModel.addTask(activeUserId!!, "Cooking 🍳", null)
                taskViewModel.addTask(activeUserId!!, "Working Out 🏋️", null)
            }
        }
    }
}

@Composable
fun AuthNavigation(profileViewModel: ProfileViewModel) {
    var showSignUp by remember { mutableStateOf(false) }
    if (showSignUp) {
        SignUpScreen(viewModel = profileViewModel, onSignInClick = { showSignUp = false })
    } else {
        LoginScreen(viewModel = profileViewModel, onSignUpClick = { showSignUp = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(taskViewModel: TaskViewModel, expenseViewModel: ExpenseViewModel, profileViewModel: ProfileViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Dashboard") }
    var selectedGameCharacter by remember { mutableStateOf<GameCharacter?>(null) }
    val activeUser by profileViewModel.userProfile.collectAsState()

    val tasks = activeUser?.let { taskViewModel.getTasksForUser(it.id).collectAsState(initial = emptyList()).value } ?: emptyList()
    val expenses = activeUser?.let { expenseViewModel.getExpensesForUser(it.id).collectAsState(initial = emptyList()).value } ?: emptyList()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = LightBackgroundColor,
        topBar = {
            TopBar(
                currentScreen = currentScreen,
                onBack = { currentScreen = "Dashboard" },
                onAddTaskClick = { showAddTaskDialog = true },
                onAddExpenseClick = { showAddExpenseDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "Dashboard" -> DashboardScreen(activeUser, onNavigate = { currentScreen = it }, onEditProfile = { showEditProfileDialog = true })
                "Tasks" -> TasksScreen(
                    tasks = tasks,
                    onTaskCheck = { task, isChecked ->
                        if (task.isChecked != isChecked) {
                            taskViewModel.updateTask(task.copy(isChecked = isChecked))
                            if (isChecked) {
                                profileViewModel.awardCoins(10)
                                scope.launch { snackbarHostState.showSnackbar("Great! You earned 10 coins!") }
                            } else {
                                profileViewModel.awardCoins(-10)
                                scope.launch { snackbarHostState.showSnackbar("10 coins deducted.") }
                            }
                        }
                    },
                    onTaskDelete = { task ->
                        if (task.isChecked) {
                            profileViewModel.awardCoins(-10)
                            scope.launch { snackbarHostState.showSnackbar("Task deleted. 10 coins deducted.") }
                        }
                        taskViewModel.deleteTask(task)
                    },
                    onTaskEdit = { taskToEdit = it }
                )
                "Expenses" -> ExpensesScreen(
                    expenses = expenses,
                    onExpenseDelete = { expenseViewModel.deleteExpense(it) },
                    onExpenseEdit = { expenseToEdit = it }
                )
                "Game Hub" -> GameHubScreen(
                    coins = activeUser?.coins ?: 0,
                    isUnlocked = activeUser?.isRockersUnlocked ?: false,
                    onPurchase = {
                        profileViewModel.purchaseRockers()
                        scope.launch {
                            if ((activeUser?.coins ?: 0) >= 100) {
                                snackbarHostState.showSnackbar("Rocker Character Unlocked! 🎸")
                            } else {
                                snackbarHostState.showSnackbar("Not enough coins!")
                            }
                        }
                    },
                    onPlayGame = { character ->
                        selectedGameCharacter = character
                        currentScreen = "Playing"
                    }
                )
                "Playing" -> selectedGameCharacter?.let { character ->
                    // Optimization: Using imported alias
                    BerlinRunnerScreen(
                        character = character,
                        onGameEnd = { coinsEarned ->
                            if (coinsEarned > 0) {
                                profileViewModel.awardCoins(coinsEarned)
                                scope.launch {
                                    snackbarHostState.showSnackbar("You earned $coinsEarned coins! 🪙")
                                }
                            }
                            currentScreen = "Game Hub"
                        },
                        onBack = { currentScreen = "Game Hub" }
                    )
                }
                "Event Finder" -> {
                    // NEW: Calling actual EventFinder screen
                    EventFinderScreen()
                }
            }
        }

        // --- DIALOGS ---
        if (showAddTaskDialog) {
            activeUser?.let {
                AddTaskDialog(
                    onTaskAdd = { text, dueDate ->
                        taskViewModel.addTask(it.id, text, dueDate)
                        showAddTaskDialog = false
                    },
                    onDismiss = { showAddTaskDialog = false }
                )
            }
        }

        taskToEdit?.let {
            EditTaskDialog(
                task = it,
                onTaskUpdate = { updatedTask ->
                    taskViewModel.updateTask(updatedTask)
                    taskToEdit = null
                },
                onDismiss = { taskToEdit = null }
            )
        }

        if (showAddExpenseDialog) {
            activeUser?.let {
                AddExpenseDialog(
                    onExpenseAdd = { name, amount, category, date ->
                        expenseViewModel.addExpense(it.id, name, amount, category, date)
                        showAddExpenseDialog = false
                    },
                    onDismiss = { showAddExpenseDialog = false }
                )
            }
        }

        expenseToEdit?.let {
            EditExpenseDialog(
                expense = it,
                onExpenseUpdate = { updatedExpense ->
                    expenseViewModel.updateExpense(updatedExpense)
                    expenseToEdit = null
                },
                onDismiss = { expenseToEdit = null }
            )
        }
        if (showEditProfileDialog) {
            activeUser?.let {
                EditProfileDialog(
                    user = it,
                    onDismiss = { showEditProfileDialog = false },
                    onProfileUpdate = { name, lastName, birth, uri, gender ->
                        profileViewModel.updateProfile(name, lastName, birth, uri, gender)
                        showEditProfileDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Profile Updated!") }
                    },
                    onSignOut = { profileViewModel.signOut() },
                    onDeleteAccount = {
                        showEditProfileDialog = false
                        showConfirmDeleteDialog = true
                    }
                )
            }
        }

        if (showConfirmDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDeleteDialog = false },
                title = { Text("Delete Account?") },
                text = { Text("Are you sure you want to permanently delete your account and all your data?") },
                confirmButton = {
                    Button(
                        onClick = {
                            profileViewModel.deleteAccount()
                            showConfirmDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("YES, DELETE") }
                },
                dismissButton = { TextButton(onClick = { showConfirmDeleteDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(currentScreen: String, onBack: () -> Unit, onAddTaskClick: () -> Unit, onAddExpenseClick: () -> Unit) {
    if (currentScreen != "Dashboard") {
        CenterAlignedTopAppBar(
            title = { Text(currentScreen, fontWeight = FontWeight.Bold, color = TextColor) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TextColor) } },
            actions = {
                if (currentScreen == "Tasks") {
                    IconButton(onClick = onAddTaskClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = AccentColor)
                    }
                } else if (currentScreen == "Expenses") {
                    IconButton(onClick = onAddExpenseClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = AccentColor)
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LightBackgroundColor)
        )
    }
}

@Composable
fun CustomAlertDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, confirmButtonText: @Composable () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextColor) },
        text = content,
        confirmButton = { Button(onConfirm, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) { confirmButtonText() } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AccentColor) } },
        containerColor = LightBackgroundColor
    )
}

@Composable
fun DashboardScreen(user: User?, onNavigate: (String) -> Unit, onEditProfile: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ProdEx", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextColor)
        Card(colors = CardDefaults.cardColors(containerColor = CardColor), modifier = Modifier.fillMaxWidth().clickable(onClick = onEditProfile)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = user?.profilePictureUri ?: R.drawable.ic_launcher_foreground, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(user?.name?.let { "$it ${user.lastName}" } ?: "My Profile", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextColor)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        MenuButton("Tasks", Icons.Default.List) { onNavigate("Tasks") }
        MenuButton("Expenses", Icons.Default.ShoppingCart) { onNavigate("Expenses") }
        MenuButton("Game Hub", Icons.Default.PlayArrow) { onNavigate("Game Hub") }
        MenuButton("Event Finder", Icons.Default.LocationOn) { onNavigate("Event Finder") }
    }
}

@Composable
fun MenuButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor), border = BorderStroke(1.dp, CardColor)) {
        Icon(icon, null, tint = AccentColor); Spacer(Modifier.width(10.dp)); Text(text, fontSize = 18.sp, color = TextColor)
    }
}

@Composable
fun LoginScreen(viewModel: ProfileViewModel, onSignUpClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    val authError by viewModel.authError.collectAsState()
    val isEmailValid = remember(email) { isValidEmail(email) }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Welcome to ProdEx", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, isError = email.isNotEmpty() && !isEmailValid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        if (email.isNotEmpty() && !isEmailValid) Text("Invalid email format.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), isError = authError != null, modifier = Modifier.fillMaxWidth())
        authError?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
        TextButton(onClick = { showForgotDialog = true }, modifier = Modifier.align(Alignment.End)) { Text("Forgot Password?") }
        Button(onClick = { viewModel.signIn(email, password) }, enabled = isEmailValid && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Login") }
        TextButton(onClick = onSignUpClick) { Text("Sign Up") }
    }
    if (showForgotDialog) ForgotPasswordDialog(viewModel, onDismiss = { showForgotDialog = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(viewModel: ProfileViewModel, onSignInClick: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var selectedQuestion by remember { mutableStateOf("What was your first pet's name?") }
    var birthDate by remember { mutableStateOf<Long?>(null) }
    var profilePictureUri by remember { mutableStateOf<Uri?>(null) }
    var gender by remember { mutableStateOf<String?>(null) }
    val isEmailValid = remember(email) { isValidEmail(email) }
    val isPasswordValid = remember(password) { isValidPassword(password) }
    val authError by viewModel.authError.collectAsState()
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { profilePictureUri = it }
    }

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { birthDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }

    Scaffold(containerColor = LightBackgroundColor) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp).padding(it), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Create Your Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                AsyncImage(model = profilePictureUri ?: R.drawable.ic_launcher_foreground, contentDescription = null, modifier = Modifier.size(100.dp).clip(CircleShape).clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentScale = ContentScale.Crop)
            }
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth()) }
            item {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, isError = !isEmailValid && email.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                if (email.isNotEmpty() && !isEmailValid) Text("Invalid email format.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
            item {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), isError = !isPasswordValid && password.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                if (!isPasswordValid && password.isNotEmpty()) Text("Min 8 chars, Upper, Lower, Number & Special.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
            item { SecurityQuestionSelector(selectedQuestion) { selectedQuestion = it } }
            item { OutlinedTextField(value = securityAnswer, onValueChange = { securityAnswer = it }, label = { Text("Security Answer") }, modifier = Modifier.fillMaxWidth()) }
            item { GenderSelector(gender) { gender = it } }
            item { OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(birthDate?.let { formatDate(it) } ?: "Select Birth Date") } }
            item {
                authError?.let { Text(it, color = Color.Red) }
                Button(onClick = { viewModel.signUp(name, lastName, email, password, birthDate, profilePictureUri?.toString(), gender, selectedQuestion, securityAnswer) }, enabled = name.isNotBlank() && lastName.isNotBlank() && isEmailValid && isPasswordValid && securityAnswer.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Create Profile") }
                TextButton(onClick = onSignInClick) { Text("Already have an account? Login") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionSelector(selectedQuestion: String, onSelect: (String) -> Unit) {
    val questions = listOf("What was your first pet's name?", "What is your mother's maiden name?", "What city were you born in?")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selectedQuestion, onValueChange = {}, readOnly = true, label = { Text("Security Question") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            questions.forEach { q -> DropdownMenuItem(text = { Text(q) }, onClick = { onSelect(q); expanded = false }) }
        }
    }
}

@Composable
fun ForgotPasswordDialog(viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var securityQuestion by remember { mutableStateOf<String?>(null) }
    var answer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()
    val isPasswordValid = remember(newPassword) { isValidPassword(newPassword) }
    val scope = rememberCoroutineScope()

    CustomAlertDialog(title = "Reset Password", onDismiss = onDismiss, onConfirm = {
        if (step == 1) { scope.launch { securityQuestion = viewModel.getSecurityQuestion(email).first(); if (securityQuestion != null) step = 2 }
        } else { if (isPasswordValid) viewModel.resetPassword(email, answer, newPassword) }
    }, confirmButtonText = { Text(if (step == 1) "Next" else "Reset") }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step == 1) { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Enter your email") }, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Question: $securityQuestion", fontWeight = FontWeight.Medium)
                OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Answer") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") }, visualTransformation = PasswordVisualTransformation(), isError = !isPasswordValid && newPassword.isNotEmpty(), modifier = Modifier.fillMaxWidth())
                if (!isPasswordValid && newPassword.isNotEmpty()) Text("Weak password format.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
            authError?.let { Text(it, color = Color.Red) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(user: User, onDismiss: () -> Unit, onProfileUpdate: (String, String, Long?, String?, String?) -> Unit, onSignOut: () -> Unit, onDeleteAccount: () -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var birthDate by remember { mutableStateOf(user.birthDate) }
    var profilePictureUri by remember { mutableStateOf(user.profilePictureUri?.let { Uri.parse(it) }) }
    var gender by remember { mutableStateOf(user.gender) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = birthDate)
    var showDatePicker by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { profilePictureUri = it } }

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { birthDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }

    CustomAlertDialog(title = "Edit Profile", onDismiss = onDismiss, onConfirm = { onProfileUpdate(name, lastName, birthDate, profilePictureUri?.toString(), gender) }, confirmButtonText = { Text("Save") }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = profilePictureUri ?: R.drawable.ic_launcher_foreground, contentDescription = null, modifier = Modifier.size(90.dp).clip(CircleShape).clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentScale = ContentScale.Crop)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
            GenderSelector(gender) { gender = it }
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(birthDate?.let { formatDate(it) } ?: "Select Birth Date") }
            Row { TextButton(onClick = onSignOut) { Text("Log Out") }; Spacer(Modifier.weight(1f)); TextButton(onClick = onDeleteAccount) { Text("Delete Account", color = Color.Red) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderSelector(selectedGender: String?, onGenderSelect: (String) -> Unit) {
    val genders = listOf("Male", "Female", "Other")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selectedGender ?: "Select Gender", onValueChange = {}, readOnly = true, label = { Text("Gender") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            genders.forEach { gender -> DropdownMenuItem(text = { Text(gender) }, onClick = { onGenderSelect(gender); expanded = false }) }
        }
    }
}

@Composable
fun TasksScreen(tasks: List<Task>, onTaskCheck: (Task, Boolean) -> Unit, onTaskDelete: (Task) -> Unit, onTaskEdit: (Task) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tasks, { it.id }) { task -> Card(Modifier.fillMaxWidth().clickable { onTaskEdit(task) }, colors = CardDefaults.cardColors(CardColor)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(task.isChecked, { onTaskCheck(task, it) })
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(task.text, color = if (task.isChecked) Color.Gray else TextColor, textDecoration = if (task.isChecked) TextDecoration.LineThrough else null)
                    Text("Due: ${task.dueDate?.let { formatDate(it) } ?: "No Date"}", fontSize = 12.sp, color = Color.Gray)
                }
                IconButton({ onTaskDelete(task) }) { Icon(Icons.Default.Delete, null, tint = AccentColor) }
            }
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(onTaskAdd: (String, Long?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return utcTimeMillis >= cal.timeInMillis
        }
    })
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }
    CustomAlertDialog(title = "Add New Task", onDismiss = onDismiss, onConfirm = { if (text.isNotBlank()) onTaskAdd(text, dueDate) }, confirmButtonText = { Text("Add") }) {
        Column { OutlinedTextField(text, { text = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth()); OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Text(dueDate?.let { formatDate(it) } ?: "Select Due Date") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(task: Task, onTaskUpdate: (Task) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(task.text) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = task.dueDate, selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return utcTimeMillis >= cal.timeInMillis
        }
    })
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }
    CustomAlertDialog(title = "Edit Task", onDismiss = onDismiss, onConfirm = { onTaskUpdate(task.copy(text = text, dueDate = dueDate)) }, confirmButtonText = { Text("Save") }) {
        Column { OutlinedTextField(text, { text = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth()); OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Text(dueDate?.let { formatDate(it) } ?: "Select Due Date") } }
    }
}

@Composable
fun ExpensesScreen(expenses: List<Expense>, onExpenseDelete: (Expense) -> Unit, onExpenseEdit: (Expense) -> Unit) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filteredExpenses = remember(expenses, selectedFilter) {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        val now = System.currentTimeMillis()
        val list = when (selectedFilter) {
            "Today" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfDay = calendar.timeInMillis
                expenses.filter { (it.date ?: it.createdAt) in startOfDay..endOfDay }
            }
            "Current Week" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val endOfWeek = calendar.timeInMillis
                expenses.filter { (it.date ?: it.createdAt) in startOfWeek..endOfWeek }
            }
            "Month" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfMonth = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endOfMonth = calendar.timeInMillis
                expenses.filter { (it.date ?: it.createdAt) in startOfMonth..endOfMonth }
            }
            else -> expenses
        }
        list.sortedBy { it.date ?: it.createdAt }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Today", "Current Week", "Month").forEach { f -> FilterChip(selectedFilter == f, { selectedFilter = f }, { Text(f) }) } }
        ExpensePieChart(filteredExpenses)
        Text("Total for $selectedFilter: €${String.format("%.2f", filteredExpenses.sumOf { it.amount })}", fontWeight = FontWeight.Bold, color = AccentColor)
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filteredExpenses, { it.id }) { expense -> Card(Modifier.fillMaxWidth().clickable { onExpenseEdit(expense) }, colors = CardDefaults.cardColors(CardColor)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(12.dp).background(Color(expense.category.colorValue), CircleShape)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(expense.name); Text(formatDate(expense.date ?: expense.createdAt), fontSize = 12.sp, color = Color.Gray) }; Text("€${expense.amount}", fontWeight = FontWeight.Bold); IconButton({ onExpenseDelete(expense) }) { Icon(Icons.Default.Delete, null, tint = AccentColor) } } } } }
    }
}

@Composable
fun ExpensePieChart(expenses: List<Expense>) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, exp) -> exp.sumOf { it.amount } }
    val total = expenses.sumOf { it.amount }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        Canvas(Modifier.size(160.dp)) { if (total > 0) { var startAngle = -90f; categoryTotals.forEach { (cat, amt) -> drawArc(Color(cat.colorValue), startAngle, (amt / total).toFloat() * 360f, true); startAngle += (amt / total).toFloat() * 360f } } else { drawArc(CardColor, 0f, 360f, true) } }
        Text("€${String.format("%.2f", total)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(onExpenseAdd: (String, Double, ExpenseCategory, Long?) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }; var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }; var customName by remember { mutableStateOf("") }; var date by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis()); var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { date = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }
    CustomAlertDialog("Add Expense", onDismiss, { val a = amount.toDoubleOrNull(); if (a != null && selectedCategory != null) onExpenseAdd(if (selectedCategory == ExpenseCategory.OTHER) customName else selectedCategory!!.displayName, a, selectedCategory!!, date) }, { Text("Add") }) {
        Column { OutlinedTextField(amount, { amount = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()); SecurityCategorySelector(selectedCategory) { selectedCategory = it }; if (selectedCategory == ExpenseCategory.OTHER) OutlinedTextField(customName, { customName = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()); OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Text(date?.let { formatDate(it) } ?: "Select Date") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(expense: Expense, onExpenseUpdate: (Expense) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }; var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(expense.category) }; var customName by remember { mutableStateOf(if (expense.category == ExpenseCategory.OTHER) expense.name else "") }; var date by remember { mutableStateOf(expense.date) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expense.date); var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { Button(onClick = { date = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) {
            CompositionLocalProvider(LocalConfiguration provides LocalConfiguration.current.apply { setLocale(Locale.UK) }) {
                DatePicker(state = datePickerState)
            }
        }
    }
    CustomAlertDialog("Edit Expense", onDismiss, { val a = amount.toDoubleOrNull(); if (a != null && selectedCategory != null) onExpenseUpdate(expense.copy(name = if (selectedCategory == ExpenseCategory.OTHER) customName else selectedCategory!!.displayName, amount = a, category = selectedCategory!!, date = date)) }, { Text("Save") }) {
        Column { OutlinedTextField(amount, { amount = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()); SecurityCategorySelector(selectedCategory) { selectedCategory = it }; if (selectedCategory == ExpenseCategory.OTHER) OutlinedTextField(customName, { customName = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth()); OutlinedButton({ showDatePicker = true }, Modifier.fillMaxWidth()) { Text(date?.let { formatDate(it) } ?: "Select Date") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCategorySelector(selectedCategory: ExpenseCategory?, onSelect: (ExpenseCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(selectedCategory?.displayName ?: "Select Category", {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { ExpenseCategory.entries.forEach { cat -> DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { onSelect(cat); expanded = false }) } }
    }
}

// THIS PART WILL BE OPTIMIZED (WILL BE DELETED AND REPLACED WITH THE REAL IMPLEMENTATION)
@Composable
fun GameHubScreen(coins: Int, isUnlocked: Boolean, onPurchase: () -> Unit, onPlayGame: (com.yigit.prodex.ui.game.GameCharacter) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Berlin Runner 🇩🇪", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AccentColor)
        Text("Wallet: $coins Coins 🪙", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Spacer(Modifier.height(8.dp))

        // Tourist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏃‍♂️ Cold Tourist", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Free Character", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("Freezing tourist exploring Berlin\nFirst time in Deutschland!", textAlign = TextAlign.Center, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPlayGame(com.yigit.prodex.ui.game.GameCharacter.TOURIST) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PLAY AS TOURIST")
                }
            }
        }

        // Berliner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isUnlocked) Color(0xFFE8F5E9) else CardColor),
            border = BorderStroke(2.dp, if (isUnlocked) Color(0xFF4CAF50) else Color.Gray)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏃‍♀️ Berlin Rocker", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (isUnlocked) "Unlocked!" else "100 Coins", fontSize = 14.sp, color = if (isUnlocked) Color(0xFF4CAF50) else Color.Red)
                Spacer(Modifier.height(8.dp))
                Text("Cool rocker in leather jacket\nRuns 30% faster! 🎸⚡", textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(12.dp))
                if (isUnlocked) {
                    Button(
                        onClick = { onPlayGame(com.yigit.prodex.ui.game.GameCharacter.ROCKER) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("PLAY AS ROCKER")
                    }
                } else {
                    OutlinedButton(
                        onClick = onPurchase,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = coins >= 100
                    ) {
                        Text("UNLOCK FOR 100 COINS")
                    }
                    if (coins < 100) {
                        Text("Complete tasks to earn coins!", fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Game Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🎮 How to Play:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("✅ Collect (Points):", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("🥨 Pretzel (10) • 🍺 Beer (20) • 🌭 Currywurst (15)", fontSize = 11.sp)
                Text("🥙 Döner (30) • 🍷 Glühwein (35) • 🎵 Vinyl (25)", fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("❌ Avoid German Obstacles:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("🚧 Baustelle • 🚇 U-Bahn • 🚊 S-Bahn • 🚄 DB Bahn", fontSize = 11.sp)
                Text("🚗 Trabant • 🚴 Bike • 🛴 Scooter • 🦊 Fox", fontSize = 11.sp)
                Text("👥 Tourists • 🐻 Berlin Bear • ❄️ Winter", fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("💰 10 points = 1 coin! 🪙", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00))
            }
        }
    }
}