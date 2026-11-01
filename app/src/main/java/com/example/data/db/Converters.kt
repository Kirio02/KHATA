package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromPartyType(value: PartyType): String = value.name

    @TypeConverter
    fun toPartyType(value: String): PartyType =
        runCatching { PartyType.valueOf(value) }.getOrDefault(PartyType.CUSTOMER)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.YOU_GAVE)

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode =
        runCatching { PaymentMode.valueOf(value) }.getOrDefault(PaymentMode.CASH)
}
