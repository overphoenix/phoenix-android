package tech.nagual.phoenix.tools.organizer.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import tech.nagual.phoenix.tools.organizer.data.model.Variant

@Dao
interface VariantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variant: Variant): Long

    @Delete
    suspend fun delete(vararg variants: Variant)

    @Transaction
    suspend fun deleteComplete(vararg variants: Variant) {
        for (variant in variants) {
            val childVariants = getAllParents(variant.categoryId, variant.id).first()
            for (child in childVariants) {
                deleteComplete(child)
            }
            delete(variant)
        }
    }

    @Update
    suspend fun update(vararg variants: Variant)

    @Query("SELECT * FROM variants WHERE id = :variantId")
    fun getById(variantId: Long): Flow<Variant?>

    @Query("SELECT * FROM variants")
    fun getAll(): Flow<List<Variant>>

    @Query("SELECT * FROM variants WHERE categoryId = :categoryId AND parentId = :variantId")
    fun getAllParents(categoryId: Long, variantId: Long): Flow<List<Variant>>

    @Query("SELECT * FROM variants WHERE categoryId = :categoryId AND parentId = :parentId")
    fun getByCategoryId(categoryId: Long, parentId: Long): Flow<List<Variant>>

    @Query("SELECT * FROM variants WHERE categoryId = :categoryId LIMIT 1")
    fun getDefaultVariantByCategoryId(categoryId: Long): Flow<Variant?>

    @Query("SELECT * FROM variants WHERE categoryId = :categoryId AND value = :value LIMIT 1")
    fun getByValueForCategory(categoryId: Long, value: String): Flow<Variant?>
}
