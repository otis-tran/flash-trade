package com.otistran.flash_trade.data.local.database.converter

import androidx.room.TypeConverter
import com.otistran.flash_trade.data.local.entity.PurchaseStatus

class PurchaseStatusConverter {
    @TypeConverter
    fun fromStatus(status: PurchaseStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): PurchaseStatus =
        PurchaseStatus.valueOf(value)
}
