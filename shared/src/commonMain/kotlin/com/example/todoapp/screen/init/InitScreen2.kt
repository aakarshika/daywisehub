package  com.example.todoapp.screen.init

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.example.todoapp.screen.NavScreens
import com.example.todoapp.screen.globalViewModels.UserViewModel

@Composable
fun InitScreen2(
    userViewModel: UserViewModel,
    userId: Long
) {
    val user by userViewModel.user.collectAsState(null)
    LaunchedEffect(Unit) {
        userViewModel.loadUserByIdOnce(userId)
    }
    if(user!=null){
        Column {
            NavScreens()
        }
    }
}