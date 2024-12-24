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
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

class InitViewModel(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository,
    private val pillarRepository: MissionRepository,
    private val todayMoodRepository: TodayMoodRepository
) : ViewModel() {


    private val _loginStatus = MutableSharedFlow<LoginStatus?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loginStatus: SharedFlow<LoginStatus?> get()  = _loginStatus

    fun loadLoginState(){
        viewModelScope.launch {
            loginRepository.getLoginStatus()
                .collect { missionList ->
                    _loginStatus.tryEmit(missionList)
                }
        }
    }
    fun signup(username: String) {
        viewModelScope.launch {
            val userId = userRepository.insertUser(User(username = username, password = "password123"))
            val login = loginRepository.insertLoginStatus(LoginStatus(userId = userId, isLoggedIn = true))
            pillarRepository.insertDefaultPillars(userId)
            todayMoodRepository.insertDefaultMoods(userId)
        }
    }
}


data class LoginStatusUiState(val item: LoginStatus?)
data class LoggedInUserUiState(val item: User?)
