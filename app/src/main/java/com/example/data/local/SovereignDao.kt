package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SovereignDao {

    // Cloud Deployments
    @Query("SELECT * FROM cloud_deployments ORDER BY isSelected DESC, id ASC")
    fun observeDeployments(): Flow<List<CloudDeploymentEntity>>

    @Query("SELECT * FROM cloud_deployments")
    suspend fun getAllDeploymentsOnce(): List<CloudDeploymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeployment(deployment: CloudDeploymentEntity): Long

    @Update
    suspend fun updateDeployment(deployment: CloudDeploymentEntity)

    @Query("UPDATE cloud_deployments SET isSelected = (id = :selectedId)")
    suspend fun selectDeployment(selectedId: Long)

    @Query("DELETE FROM cloud_deployments WHERE id = :id")
    suspend fun deleteDeployment(id: Long)

    // Persona Profiles
    @Query("SELECT * FROM persona_profiles ORDER BY isSelected DESC, id ASC")
    fun observePersonas(): Flow<List<PersonaProfileEntity>>

    @Query("SELECT * FROM persona_profiles")
    suspend fun getAllPersonasOnce(): List<PersonaProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaProfileEntity): Long

    @Update
    suspend fun updatePersona(persona: PersonaProfileEntity)

    @Query("UPDATE persona_profiles SET isSelected = (id = :selectedId)")
    suspend fun selectPersona(selectedId: Long)

    @Query("DELETE FROM persona_profiles WHERE id = :id")
    suspend fun deletePersona(id: Long)

    // Conversation Sessions
    @Query("SELECT * FROM conversation_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ConversationSessionEntity>>

    @Query("SELECT * FROM conversation_sessions")
    suspend fun getAllSessionsOnce(): List<ConversationSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSessionEntity): Long

    @Update
    suspend fun updateSession(session: ConversationSessionEntity)

    @Query("DELETE FROM conversation_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM session_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    // Session Messages
    @Query("SELECT * FROM session_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessagesForSession(sessionId: Long): Flow<List<SessionMessageEntity>>

    @Query("SELECT * FROM session_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<SessionMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SessionMessageEntity): Long

    @Update
    suspend fun updateMessage(message: SessionMessageEntity)
}
