package com.yigit.prodex.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigit.prodex.data.User
import com.yigit.prodex.data.UserDao
import com.yigit.prodex.util.PasswordHasher
import com.yigit.prodex.util.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val userDao: UserDao, private val sessionManager: SessionManager) : ViewModel() {

    val activeUserId: StateFlow<Int?> = sessionManager.activeUserId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val userProfile: StateFlow<User?> = activeUserId.flatMapLatest { userId ->
        userId?.let { userDao.getUserById(it) } ?: flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun signUp(
        name: String, 
        lastName: String, 
        email: String, 
        password: String, 
        birthDate: Long?, 
        profilePictureUri: String?, 
        gender: String?,
        securityQuestion: String,
        securityAnswer: String
    ) {
        viewModelScope.launch {
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                _authError.value = "This email is already registered."
            } else {
                val salt = PasswordHasher.generateSalt()
                val hashedPassword = PasswordHasher.hashPassword(password, salt)
                val hashedAnswer = PasswordHasher.hashPassword(securityAnswer.lowercase(), salt)
                val newUser = User(
                    name = name,
                    lastName = lastName,
                    email = email,
                    hashedPassword = hashedPassword,
                    salt = salt,
                    birthDate = birthDate,
                    profilePictureUri = profilePictureUri,
                    gender = gender,
                    coins = 0,
                    isRockersUnlocked = false,
                    securityQuestion = securityQuestion,
                    securityAnswer = hashedAnswer
                )
                val newUserId = userDao.insertUser(newUser)
                sessionManager.setActiveUserId(newUserId.toInt())
                _authError.value = null
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            val user = userDao.getUserByEmail(email)
            if (user == null) {
                _authError.value = "User not found."
            } else {
                if (PasswordHasher.verifyPassword(password, user.salt, user.hashedPassword)) {
                    sessionManager.setActiveUserId(user.id)
                    _authError.value = null
                } else {
                    _authError.value = "Incorrect password."
                }
            }
        }
    }

    fun resetPassword(email: String, answer: String, newPassword: String) {
        viewModelScope.launch {
            val user = userDao.getUserByEmail(email)
            if (user == null) {
                _authError.value = "User not found."
            } else {
                val hashedProvidedAnswer = PasswordHasher.hashPassword(answer.lowercase(), user.salt)
                if (hashedProvidedAnswer == user.securityAnswer) {
                    val newHash = PasswordHasher.hashPassword(newPassword, user.salt)
                    userDao.updateUser(user.copy(hashedPassword = newHash))
                    _authError.value = null
                    // Automatically sign in after reset
                    sessionManager.setActiveUserId(user.id)
                } else {
                    _authError.value = "Incorrect security answer."
                }
            }
        }
    }

    fun getSecurityQuestion(email: String): Flow<String?> = flow {
        val user = userDao.getUserByEmail(email)
        emit(user?.securityQuestion)
    }
    
    fun updateProfile(name: String, lastName: String, birthDate: Long?, profilePictureUri: String?, gender: String?) {
        viewModelScope.launch {
            userProfile.value?.let { currentUser ->
                val updatedUser = currentUser.copy(
                    name = name,
                    lastName = lastName,
                    birthDate = birthDate,
                    profilePictureUri = profilePictureUri,
                    gender = gender
                )
                userDao.updateUser(updatedUser)
            }
        }
    }

    fun awardCoins(amount: Int) {
        viewModelScope.launch {
            activeUserId.value?.let { userId ->
                userDao.addCoins(userId, amount)
            }
        }
    }

    fun purchaseRockers() {
        viewModelScope.launch {
            val user = userProfile.value
            if (user != null && user.coins >= 100 && !user.isRockersUnlocked) {
                userDao.unlockRockers(user.id, 100)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.setActiveUserId(null)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            userProfile.value?.let {
                userDao.deleteUser(it)
                signOut()
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }
}

class ProfileViewModelFactory(private val userDao: UserDao, private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userDao, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
