package  com.example.todoapp.screen.init
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.user.User
import com.example.todoapp.repo.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.todoapp.db.data.loginstatus.LoginStatus
import com.example.todoapp.repo.LoginRepository
import com.example.todoapp.repo.MissionRepository
import kotlinx.coroutines.flow.MutableStateFlow

class InitViewModel(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository,
    private val pillarRepository: MissionRepository,
) : ViewModel() {


    private val _loginStatus = MutableStateFlow<LoginStatus?>(null)
    val loginStatus: StateFlow<LoginStatus?> = _loginStatus


    init {
        loadLoginState()
    }
    fun loadLoginState(){
        viewModelScope.launch {
            _loginStatus.value = loginRepository.getLoginStatus()
        }
    }
    fun signup(username: String) {
        viewModelScope.launch {
            val user = userRepository.insertUser(User(username = username, password = "password123"))
            val login = loginRepository.insertLoginStatus(LoginStatus(userId = user.id, isLoggedIn = true))
            pillarRepository.insertDefaultPillars(user)
            loadLoginState()
        }
    }
}

data class LoginStatusUiState(val item: LoginStatus?)
data class LoggedInUserUiState(val item: User?)
