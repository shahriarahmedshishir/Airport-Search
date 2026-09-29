package com.example.flightsearch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile

@Database(entities = [Airport::class, Favorite::class], version = 2, exportSchema = false)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao

    companion object {
        @Volatile
        private var Instance: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_search_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                    .also { Instance = it }
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Instance?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.airportDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Instance?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (database.airportDao().getAirportCount() == 0) {
                            populateDatabase(database.airportDao())
                        }
                    }
                }
            }

            suspend fun populateDatabase(airportDao: AirportDao) {
                val sampleAirports = listOf(
                    Airport(iataCode = "FCO", name = "Leonardo da Vinci–Fiumicino Airport", passengers = 43532573),
                    Airport(iataCode = "LHR", name = "London Heathrow Airport", passengers = 80886586),
                    Airport(iataCode = "MUC", name = "Franz Josef Strauss Airport", passengers = 47959885),
                    Airport(iataCode = "JFK", name = "John F. Kennedy International Airport", passengers = 62551072),
                    Airport(iataCode = "SFO", name = "San Francisco International Airport", passengers = 57488023),
                    Airport(iataCode = "CDG", name = "Charles de Gaulle Airport", passengers = 76150000),
                    Airport(iataCode = "NRT", name = "Narita International Airport", passengers = 44344739),
                    Airport(iataCode = "ARN", name = "Stockholm Arlanda Airport", passengers = 25642703),
                    Airport(iataCode = "HEL", name = "Helsinki-Vantaa Airport", passengers = 20847000),
                    Airport(iataCode = "MAD", name = "Adolfo Suárez Madrid–Barajas Airport", passengers = 61784035),
                    Airport(iataCode = "LAX", name = "Los Angeles International Airport", passengers = 88068613),
                    Airport(iataCode = "ORD", name = "O'Hare International Airport", passengers = 84649115),
                    Airport(iataCode = "DXB", name = "Dubai International Airport", passengers = 86400000),
                    Airport(iataCode = "HND", name = "Tokyo Haneda Airport", passengers = 85500000),
                    Airport(iataCode = "AMS", name = "Amsterdam Airport Schiphol", passengers = 71700000)
                )
                airportDao.insertAirports(sampleAirports)
            }
        }
    }
}
