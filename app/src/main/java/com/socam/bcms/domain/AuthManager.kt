package com.socam.bcms.domain

import android.content.Context
import com.socam.bcms.data.database.DatabaseManager
import com.socam.bcms.database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Authentication manager for local user validation and session management
 * Handles password hashing, user authentication, and session tracking
 */
class AuthManager(private val context: Context) {
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private var currentUser: User? = null
    
    /**
     * Authenticate user with username and password
     */
    suspend fun authenticateUser(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val user = databaseManager.database.userQueries
                .selectByUsername(username)
                .executeAsOneOrNull()
            
            if (user == null) {
                return@withContext AuthResult.Failure("User not found")
            }
            
            if (user.is_active == 0L) {
                return@withContext AuthResult.Failure("User account is deactivated")
            }
            
            // Verify password with stored hash
            val isPasswordValid = verifyPassword(password, user.password_hash, user.salt)
            
            if (isPasswordValid) {
                currentUser = user
                AuthResult.Success(user)
            } else {
                AuthResult.Failure("Invalid password")
            }
        } catch (e: Exception) {
            AuthResult.Failure("Authentication failed: ${e.message}")
        }
    }
    
    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): User? = currentUser
    
    /**
     * Check if user is currently authenticated
     */
    fun isAuthenticated(): Boolean = currentUser != null
    
    /**
     * Logout current user
     */
    fun logout(): Unit {
        currentUser = null
    }
    
    /**
     * Validate token for API authentication
     */
    suspend fun validateToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = databaseManager.database.userQueries
                .selectByToken(token)
                .executeAsOneOrNull()
            
            user != null && user.is_active > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create new user with encrypted password
     */
    suspend fun createUser(
        username: String,
        password: String,
        fullName: String,
        email: String?,
        department: String?,
        role: String = "USER"
    ): CreateUserResult = withContext(Dispatchers.IO) {
        try {
            // Check if username already exists
            val existingUser = databaseManager.database.userQueries
                .selectByUsername(username)
                .executeAsOneOrNull()
            
            if (existingUser != null) {
                return@withContext CreateUserResult.Failure("Username already exists")
            }
            
            // Generate salt and hash password
            val salt = generateSalt()
            val passwordHash = hashPassword(password, salt)
            val token = generateToken()
            
            // Insert new user
            databaseManager.database.userQueries.insertUser(
                username = username,
                password_hash = passwordHash,
                salt = salt,
                token = token,
                role = role,
                full_name = fullName,
                email = email,
                department = department
            )
            
            CreateUserResult.Success("User created successfully")
        } catch (e: Exception) {
            CreateUserResult.Failure("Failed to create user: ${e.message}")
        }
    }
    
    /**
     * Update user password
     */
    suspend fun updatePassword(userId: Long, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val salt = generateSalt()
            val passwordHash = hashPassword(newPassword, salt)
            
            databaseManager.database.userQueries.updateUserPassword(
                password_hash = passwordHash,
                salt = salt,
                id = userId
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate secure salt for password hashing
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Hash password with salt using PBKDF2
     */
    private fun hashPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify password against stored hash
     */
    private fun verifyPassword(password: String, storedHash: String, salt: String): Boolean {
        val hashedInput = hashPassword(password, salt)
        return hashedInput == storedHash
    }
    
    /**
     * Generate random token for API authentication
     */
    private fun generateToken(): String {
        val random = SecureRandom()
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        return tokenBytes.joinToString("") { "%02x".format(it) }
    }
    
    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }
    
    sealed class CreateUserResult {
        data class Success(val message: String) : CreateUserResult()
        data class Failure(val message: String) : CreateUserResult()
    }
}
