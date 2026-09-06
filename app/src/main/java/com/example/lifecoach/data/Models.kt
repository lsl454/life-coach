package com.example.lifecoach.data

import androidx.room.*

@Entity(tableName="daily_record")
data class DailyRecord(@PrimaryKey val date:String, val wakeTime:String?=null, val sleepTime:String?=null, val clarity:Int?=null, val cigarettes:Int=0, val walked:Boolean=false, val studyMinutes:Int=0, val mood:String?=null, val weight:Float?=null, val adultContent:Boolean=false, val afterWorkChoice:String?=null, val dailyReport:String?=null, val completionRate:Int=0)
@Entity(tableName="task", indices=[Index(value=["date","sortOrder"], unique=true)])
data class Task(@PrimaryKey(autoGenerate=true) val id:Long=0, val date:String, val title:String, val done:Boolean=false, val rollCount:Int=0, val createdBy:String="USER", val sortOrder:Int=0)
@Entity(tableName="pending_prompt", indices=[Index(value=["date","slotKey"], unique=true)])
data class PendingPrompt(@PrimaryKey(autoGenerate=true) val id:Long=0,val date:String,val slotKey:String,val scheduledTime:Long,val createdTime:Long,val isCatchUp:Boolean=false,val type:String,val question:String,val field:String?=null,val optionsJson:String?=null,val status:String="PENDING",val answer:String?=null,val answeredTime:Long?=null)
@Entity(tableName="confusion")
data class Confusion(@PrimaryKey(autoGenerate=true) val id:Long=0,val date:String,val content:String,val resolved:Boolean=false)
@Entity(tableName="chat_message")
data class ChatMessage(@PrimaryKey(autoGenerate=true) val id:Long=0,val timestamp:Long,val role:String,val content:String)

@Dao interface DailyDao { @Query("SELECT * FROM daily_record WHERE date=:date") suspend fun get(date:String):DailyRecord?; @Query("SELECT * FROM daily_record ORDER BY date DESC LIMIT :limit") suspend fun recent(limit:Int):List<DailyRecord>; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(row:DailyRecord) }
@Dao interface TaskDao { @Query("SELECT * FROM task WHERE date=:date ORDER BY sortOrder") suspend fun forDate(date:String):List<Task>; @Query("SELECT * FROM task ORDER BY date DESC, sortOrder") suspend fun all():List<Task>; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(row:Task); @Delete suspend fun delete(row:Task) }
@Dao interface PromptDao { @Query("SELECT * FROM pending_prompt WHERE status='PENDING' ORDER BY scheduledTime") suspend fun pending():List<PendingPrompt>; @Query("SELECT * FROM pending_prompt WHERE date=:date AND status='PENDING' ORDER BY scheduledTime") suspend fun pending(date:String):List<PendingPrompt>; @Query("SELECT COUNT(*) FROM pending_prompt WHERE date=:date AND slotKey=:slot") suspend fun count(date:String,slot:String):Int; @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insert(row:PendingPrompt):Long; @Query("UPDATE pending_prompt SET status=:status,answer=:answer,answeredTime=:at WHERE id=:id") suspend fun answer(id:Long,status:String,answer:String?,at:Long); @Query("SELECT * FROM pending_prompt ORDER BY scheduledTime DESC LIMIT :limit") suspend fun recent(limit:Int):List<PendingPrompt> }
@Dao interface ChatDao { @Query("SELECT * FROM chat_message ORDER BY timestamp") fun stream():kotlinx.coroutines.flow.Flow<List<ChatMessage>>; @Insert suspend fun add(row:ChatMessage) }
@Database(entities=[DailyRecord::class,Task::class,PendingPrompt::class,Confusion::class,ChatMessage::class],version=1,exportSchema=false)
abstract class CoachDatabase:RoomDatabase(){ abstract fun daily():DailyDao; abstract fun tasks():TaskDao; abstract fun prompts():PromptDao; abstract fun chat():ChatDao
 companion object { @Volatile private var instance:CoachDatabase?=null; fun get(ctx:android.content.Context)=instance?:synchronized(this){instance?:Room.databaseBuilder(ctx.applicationContext,CoachDatabase::class.java,"coach.db").fallbackToDestructiveMigration().build().also{instance=it}} }
}
