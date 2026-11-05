package com.example.hello.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.hello.data.local.entity.CaptureFlowComponentEntity
import com.example.hello.data.local.entity.CaptureFlowPresetEntity
import com.example.hello.data.local.entity.CaptureFlowPresetWithComponents
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureFlowDao {

    @Transaction
    @Query("SELECT * FROM capture_flow_presets ORDER BY displayOrder ASC")
    fun getAllPresetsWithComponentsFlow(): Flow<List<CaptureFlowPresetWithComponents>>

    @Transaction
    @Query("SELECT * FROM capture_flow_presets WHERE id = :presetId")
    suspend fun getPresetWithComponents(presetId: String): CaptureFlowPresetWithComponents?

    @Query("SELECT * FROM capture_flow_presets")
    suspend fun getAllPresetEntities(): List<CaptureFlowPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<CaptureFlowPresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<CaptureFlowComponentEntity>)

    @Update
    suspend fun updatePreset(preset: CaptureFlowPresetEntity)

    @Update
    suspend fun updateComponent(component: CaptureFlowComponentEntity)

    @Query("DELETE FROM capture_flow_components WHERE presetId = :presetId")
    suspend fun deleteComponentsForPreset(presetId: String)

    @Query("DELETE FROM capture_flow_presets WHERE id = :presetId AND isSystemPreset = 0")
    suspend fun deleteCustomPreset(presetId: String)
}
