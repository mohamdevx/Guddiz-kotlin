package com.guddiz.app.data.repository

import com.guddiz.app.data.local.dao.ExpenseDao
import com.guddiz.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    fun observeExpenses(query: String = "", category: String = ""): Flow<List<ExpenseEntity>> =
        expenseDao.observeFiltered(query.trim(), category)

    suspend fun getExpense(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun save(entity: ExpenseEntity): Long {
        val now = System.currentTimeMillis()
        return if (entity.id == 0L) {
            expenseDao.insert(entity.copy(createdAt = now, updatedAt = now))
        } else {
            expenseDao.update(entity.copy(updatedAt = now))
            entity.id
        }
    }

    suspend fun softDelete(id: Long) {
        expenseDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Long) {
        expenseDao.restore(id, System.currentTimeMillis())
    }

    suspend fun hardDelete(id: Long) = expenseDao.hardDelete(id)

    suspend fun duplicate(id: Long): Long? {
        val original = expenseDao.getById(id) ?: return null
        val now = System.currentTimeMillis()
        return expenseDao.insert(
            original.copy(
                id = 0L,
                expenseDate = com.guddiz.app.core.util.DateUtils.today(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun observeDeleted(): Flow<List<ExpenseEntity>> = expenseDao.observeDeleted()
    suspend fun purgeDeletedBefore(cutoff: Long): List<ExpenseEntity> =
        expenseDao.findDeletedBefore(cutoff)

    fun observeTotalExpenses(): Flow<Long> = expenseDao.observeTotalExpenses()
    fun observeExpensesSince(fromMillis: Long): Flow<Long> =
        expenseDao.observeExpensesSince(fromMillis)
    suspend fun recentVendors(limit: Int = 6): List<String> = expenseDao.recentVendors(limit)
}
