package com.animeboynz.kmd.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.animeboynz.kmd.database.dao.EmployeeDao
import com.animeboynz.kmd.database.entities.EmployeeEntity

@Database(entities = [
    EmployeeEntity::class],
    version = 2
)
abstract class ALMDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
}
