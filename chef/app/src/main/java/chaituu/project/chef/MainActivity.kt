package chaituu.project.chef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaituu.project.chef.api.RetrofitClient
import chaituu.project.chef.models.LoginRequest
import chaituu.project.chef.models.RegisterRequest
import chaituu.project.chef.models.JoinCafeRequest
import chaituu.project.chef.models.Order
import chaituu.project.chef.models.OrderStatusUpdate
import chaituu.project.chef.ui.theme.ChefTheme
import chaituu.project.chef.utils.PreferenceManager
import chaituu.project.chef.utils.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChefTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChefApp(
                        context = this@MainActivity,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

private enum class AuthMode {
    Login,
    SignUp
}

@Composable
fun ChefApp(context: android.content.Context, modifier: Modifier = Modifier) {
    val prefManager = remember { PreferenceManager(context) }
    val token = remember { mutableStateOf(prefManager.getToken()) }
    val cafeId = remember { mutableStateOf(prefManager.getCafeId()) }
    val chefName = remember { mutableStateOf(prefManager.getChefName()) }
    val authMode = remember { mutableStateOf(AuthMode.Login) }

    // Ensure Retrofit has the token immediately after composition to avoid
    // a race where OrdersScreen issues requests before the LaunchedEffect
    // coroutine has set the token on the client.
    SideEffect {
        if (!token.value.isNullOrBlank()) {
            RetrofitClient.setToken(token.value!!)
        } else {
            RetrofitClient.clearToken()
        }
    }

    LaunchedEffect(token.value) {
        if (!token.value.isNullOrBlank()) {
            RetrofitClient.setToken(token.value!!)
        } else {
            RetrofitClient.clearToken()
        }
    }

    if (token.value == null) {
        when (authMode.value) {
            AuthMode.Login -> {
                LoginScreen(
                    onLoginSuccess = { loginToken, cafe, name ->
                        token.value = loginToken
                        cafeId.value = cafe
                        chefName.value = name
                        RetrofitClient.setToken(loginToken)
                        prefManager.saveToken(loginToken)
                        // save cafeId only when non-null
                        cafe?.let { prefManager.saveCafeId(it) }
                        prefManager.saveChefName(name)
                    },
                    onGoToSignup = { authMode.value = AuthMode.SignUp },
                    modifier = modifier
                )
            }
            AuthMode.SignUp -> {
                SignupScreen(
                    onSignupSuccess = { signupToken, cafe, name ->
                        token.value = signupToken
                        cafeId.value = cafe
                        chefName.value = name
                        RetrofitClient.setToken(signupToken)
                        prefManager.saveToken(signupToken)
                        cafe?.let { prefManager.saveCafeId(it) }
                        prefManager.saveChefName(name)
                    },
                    onGoToLogin = { authMode.value = AuthMode.Login },
                    modifier = modifier
                )
            }
        }
    } else if (cafeId.value == null) {
        JoinCafeScreen(
            chefName = chefName.value ?: "",
            onJoinSuccess = { cafe, cafeName ->
                cafeId.value = cafe
                prefManager.saveCafeId(cafe)
            },
            onLogout = {
                token.value = null
                cafeId.value = null
                chefName.value = null
                prefManager.clear()
                RetrofitClient.clearToken()
            },
            modifier = modifier
        )
    } else {
        OrdersScreen(
            cafeId = cafeId.value,
            chefName = chefName.value ?: "",
            onLogout = {
                token.value = null
                cafeId.value = null
                chefName.value = null
                prefManager.clear()
                RetrofitClient.clearToken()
            },
            modifier = modifier
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String?, String) -> Unit,
    onGoToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3B82F6))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            "Expresso Chef",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Chef Login",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        error = ""
                        isLoading = true
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.login(
                                        LoginRequest(email, password)
                                    )
                                }
                                if (response.user.role == "chef") {
                                    onLoginSuccess(response.token, response.user.cafeId, response.user.name)
                                } else {
                                    error = "This app is for chefs only"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Login failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
                ) {
                    Text(if (isLoading) "Logging in..." else "Login")
                }

                TextButton(
                    onClick = onGoToSignup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Create new account")
                }
            }
        }
    }
}

@Composable
fun SignupScreen(
    onSignupSuccess: (String, String?, String) -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3B82F6))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            "Expresso Chef",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Chef Sign Up",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        error = ""
                        isLoading = true
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.register(
                                        RegisterRequest(name, email, password)
                                    )
                                }
                                if (response.user.role == "chef") {
                                    onSignupSuccess(response.token, response.user.cafeId, response.user.name)
                                } else {
                                    error = "This app is for chefs only"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Signup failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
                ) {
                    Text(if (isLoading) "Creating..." else "Create Account")
                }

                TextButton(
                    onClick = onGoToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Back to login")
                }
            }
        }
    }
}

@Composable
fun JoinCafeScreen(
    chefName: String,
    onJoinSuccess: (String, String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF3B82F6))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            "Expresso Chef",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Join Cafe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    "Welcome, $chefName",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Cafe Code") },
                    placeholder = { Text("Enter 6-digit code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Button(
                    onClick = {
                        error = ""
                        isLoading = true
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.joinCafeWithCode(
                                        JoinCafeRequest(code)
                                    )
                                }
                                onJoinSuccess(response.data.cafeId, response.data.cafeName)
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to join cafe"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && code.isNotEmpty()
                ) {
                    Text(if (isLoading) "Joining..." else "Join Cafe")
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(
    cafeId: String?,
    chefName: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val socketManager = remember { SocketManager.getInstance() }

    suspend fun fetchOrders() {
        if (cafeId == null) {
            orders = emptyList()
            isLoading = false
            error = "No cafe assigned. Waiting for assignment."
            return
        }

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getOrders(cafeId)
            }
            orders = response.data
            error = ""
        } catch (e: Exception) {
            error = e.message ?: "Failed to load orders"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(cafeId) {
        if (cafeId != null) {
            withContext(Dispatchers.IO) {
                socketManager.connect(cafeId)
            }
            socketManager.on("order-updated") { data ->
                scope.launch {
                    fetchOrders()
                }
            }

            fetchOrders()
        } else {
            // No cafe assigned yet
            isLoading = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF3B82F6)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text("Expresso", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Hello, $chefName", fontSize = 14.sp, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            isLoading = true
                            fetchOrders()
                        }
                    }) {
                        Text("Refresh", color = Color.White)
                    }
                    Button(onClick = {
                        socketManager.disconnect()
                        onLogout()
                    }) {
                        Text("Logout")
                    }
                }
            }
        }

        if (error.isNotEmpty()) {
            Text(
                error,
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No pending orders", fontSize = 18.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        onStatusChange = { newStatus ->
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.updateOrderStatus(
                                            order._id,
                                            OrderStatusUpdate(newStatus)
                                        )
                                    }
                                    fetchOrders()
                                } catch (e: Exception) {
                                    error = e.message ?: "Failed to update status"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Table: ${order.tableId.tableNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Total: ₹${order.totalPrice}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Surface(
                    color = when (order.status) {
                        "pending" -> Color(0xFF3B82F6)
                        "preparing" -> Color(0xFFF59E0B)
                        "ready" -> Color(0xFF10B981)
                        else -> Color.Gray
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        order.status.uppercase(),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Items:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                order.items.forEach { item ->
                    Text("• ${item.name} x ${item.qty}", fontSize = 14.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    "pending" -> {
                        Button(
                            onClick = { onStatusChange("preparing") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Preparing")
                        }
                    }
                    "preparing" -> {
                        Button(
                            onClick = { onStatusChange("ready") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("Mark Ready")
                        }
                    }
                }
            }
        }
    }
}
