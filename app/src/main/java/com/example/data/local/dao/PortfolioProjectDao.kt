package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PortfolioProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Photography & Video Portfolio Projects.
 * Exposes reactive [Flow] queries and suspend mutation methods.
 */
@Dao
interface PortfolioProjectDao {

    @Query("SELECT * FROM portfolio_projects ORDER BY isFeatured DESC, updatedAt DESC")
    fun observeAllProjects(): Flow<List<PortfolioProjectEntity>>

    @Query("SELECT * FROM portfolio_projects WHERE mediaType = :mediaType ORDER BY isFeatured DESC, updatedAt DESC")
    fun observeProjectsByMediaType(mediaType: String): Flow<List<PortfolioProjectEntity>>

    @Query("SELECT * FROM portfolio_projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoriteProjects(): Flow<List<PortfolioProjectEntity>>

    @Query("SELECT * FROM portfolio_projects WHERE category = :category ORDER BY isFeatured DESC, updatedAt DESC")
    fun observeProjectsByCategory(category: String): Flow<List<PortfolioProjectEntity>>

    @Query("SELECT COUNT(*) FROM portfolio_projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT * FROM portfolio_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): PortfolioProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: PortfolioProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<PortfolioProjectEntity>): List<Long>

    @Update
    suspend fun updateProject(project: PortfolioProjectEntity)

    @Query("UPDATE portfolio_projects SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(
        id: Long,
        isFavorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteProject(project: PortfolioProjectEntity)

    @Query("DELETE FROM portfolio_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM portfolio_projects")
    suspend fun clearAllProjects()
}
