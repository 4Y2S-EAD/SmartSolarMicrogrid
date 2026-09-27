package com.smartsolar.microgrid.member1.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.smartsolar.microgrid.network.models.UserProfileResponse

class ProfileDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "SmartSolarProfile.db"
        const val TABLE_PROFILE = "UserProfile"

        const val COLUMN_NIC = "nic"
        const val COLUMN_FULL_NAME = "fullName"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phoneNumber"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_ROLE = "role"
        const val COLUMN_STATUS = "accountStatus"
        const val COLUMN_IS_APPROVED = "isApproved"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE " + TABLE_PROFILE + "("
                + COLUMN_NIC + " TEXT PRIMARY KEY,"
                + COLUMN_FULL_NAME + " TEXT,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_PHONE + " TEXT,"
                + COLUMN_ADDRESS + " TEXT,"
                + COLUMN_ROLE + " TEXT,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_IS_APPROVED + " INTEGER" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE)
        onCreate(db)
    }

    fun saveProfile(profile: UserProfileResponse) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NIC, profile.nic)
            put(COLUMN_FULL_NAME, profile.fullName)
            put(COLUMN_EMAIL, profile.email)
            put(COLUMN_PHONE, profile.phoneNumber)
            put(COLUMN_ADDRESS, profile.address)
            put(COLUMN_ROLE, profile.role)
            put(COLUMN_STATUS, profile.accountStatus)
            put(COLUMN_IS_APPROVED, if (profile.isApproved) 1 else 0)
        }
        
        val cursor = db.query(TABLE_PROFILE, arrayOf(COLUMN_NIC), "$COLUMN_NIC=?", arrayOf(profile.nic), null, null, null)
        if (cursor.moveToFirst()) {
            db.update(TABLE_PROFILE, values, "$COLUMN_NIC=?", arrayOf(profile.nic))
        } else {
            db.insert(TABLE_PROFILE, null, values)
        }
        cursor.close()
        db.close()
    }

    fun getProfile(nic: String): UserProfileResponse? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_PROFILE, null, "$COLUMN_NIC=?", arrayOf(nic), null, null, null)
        
        var profile: UserProfileResponse? = null
        if (cursor.moveToFirst()) {
            profile = UserProfileResponse(
                nic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NIC)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)),
                accountStatus = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                isApproved = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_APPROVED)) == 1
            )
        }
        cursor.close()
        db.close()
        return profile
    }
}
