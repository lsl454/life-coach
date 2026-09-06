package com.example.lifecoach.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dayFmt=SimpleDateFormat("yyyy-MM-dd",Locale.US)
fun today():String=dayFmt.format(Date())
data class Slot(val key:String,val hour:Int,val minute:Int,val expire:Int,val type:String,val question:String,val field:String?,val options:List<String> = emptyList())
object Schedule { val work= listOf(Slot("WAKE",7,30,12,"SCALE","现在几分精神？","clarity",listOf("1","2","3","4","5")),Slot("PLAN",7,35,10,"MESSAGE","教练排了今天三件事。","",listOf("就这样","我改")),Slot("NOON",12,30,8,"YESNO","上午那件事干成了吗？","mood",listOf("干了","没有")),Slot("PUSH",15,30,0,"MESSAGE","别把下午也耗掉。现在做一个能在 15 分钟内完成的小动作。",null),Slot("AFTER_WORK",17,45,4,"CHOICE","到家了，今晚准备怎么处理这段时间？","afterWorkChoice",listOf("下楼走 20 分钟","学 25 分钟","我要滑了")),Slot("STUDY",20,0,6,"NUMBER","今天学了多久？","studyMinutes",listOf("0","25","50","90")),Slot("WRAP",22,30,24,"TEXT","今天一句话。","mood"),Slot("REPORT",23,0,36,"MESSAGE","今天到这里。看一眼数据，明天照做。",null)) }

class CoachRepository(private val ctx:Context){
 private val db=CoachDatabase.get(ctx); private val gson=Gson()
 private val prefs by lazy { EncryptedSharedPreferences.create(ctx,"secure",MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM) }
 fun apiKey()=prefs.getString("deepseek_key","") ?: ""; fun setApiKey(v:String)=prefs.edit().putString("deepseek_key",v.trim()).apply()
 suspend fun recordWake(){ val d=today(); val old=db.daily().get(d); if(old?.wakeTime==null) db.daily().upsert((old?:DailyRecord(d)).copy(wakeTime=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()))) }
 suspend fun quickSmoke(){withContext(Dispatchers.IO){val d=today();val r=db.daily().get(d)?:DailyRecord(d);db.daily().upsert(r.copy(cigarettes=r.cigarettes+1))}}
 suspend fun quickWalk(){withContext(Dispatchers.IO){val d=today();val r=db.daily().get(d)?:DailyRecord(d);db.daily().upsert(r.copy(walked=true))}}
 private fun scheduled(day:String,slot:Slot):Long { val c=Calendar.getInstance(); c.time=dayFmt.parse(day)!!; c.set(Calendar.HOUR_OF_DAY,slot.hour); c.set(Calendar.MINUTE,slot.minute); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.timeInMillis }
 suspend fun carryOver(){withContext(Dispatchers.IO){val c=Calendar.getInstance();c.add(Calendar.DAY_OF_YEAR,-1);val yesterday=dayFmt.format(c.time);val ts=db.tasks().forDate(yesterday);val todayTasks=db.tasks().forDate(today());for(t in ts.filter{!it.done} .take(3-todayTasks.size.coerceAtMost(3))){db.tasks().upsert(t.copy(id=0,date=today(),rollCount=t.rollCount+1,sortOrder=todayTasks.size))}}}
 suspend fun catchUp(now:Long=System.currentTimeMillis()){ withContext(Dispatchers.IO){ val cal=Calendar.getInstance(); val days=(0..1).map{Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-it)}.let{dayFmt.format(it.time)}}; for(day in days) for(slot in Schedule.work){ val at=scheduled(day,slot); if(at>now||db.prompts().count(day,slot.key)>0) continue; val late=(now-at)/3600000; if(slot.expire>0&&late>slot.expire) continue; db.prompts().insert(PendingPrompt(date=day,slotKey=slot.key,scheduledTime=at,createdTime=now,isCatchUp=late>0,type=slot.type,question=slot.question,field=slot.field,optionsJson=gson.toJson(slot.options))) } } }
 suspend fun pending()=db.prompts().pending(); suspend fun answer(p:PendingPrompt,answer:String?,skipped:Boolean=false){ withContext(Dispatchers.IO){ db.prompts().answer(p.id,if(skipped)"SKIPPED" else "ANSWERED",answer,System.currentTimeMillis()); val d=db.daily().get(p.date)?:DailyRecord(p.date); val next=when(p.field){"clarity"->d.copy(clarity=answer?.toIntOrNull());"studyMinutes"->d.copy(studyMinutes=answer?.toIntOrNull()?:0);"walked"->d.copy(walked=answer=="走了"||answer=="干了");"adultContent"->d.copy(adultContent=answer=="没有");"afterWorkChoice"->d.copy(afterWorkChoice=answer);"sleepTime"->d.copy(sleepTime=answer);"mood"->d.copy(mood=answer);else->d}; db.daily().upsert(next) } }
 suspend fun tasks()=db.tasks().forDate(today()); suspend fun toggleTask(t:Task)=db.tasks().upsert(t.copy(done=!t.done)); suspend fun addTask(title:String){val ts=db.tasks().forDate(today());if(ts.size<3)db.tasks().upsert(Task(date=today(),title=title,sortOrder=ts.size))}; suspend fun rollTask(t:Task){db.tasks().upsert(t.copy(date=today(),rollCount=t.rollCount+1,done=false))}
 suspend fun recentRecords()=db.daily().recent(30); fun chat()=db.chat().stream(); suspend fun addChat(m:ChatMessage)=db.chat().add(m)
 suspend fun exportJson():String=withContext(Dispatchers.IO){gson.toJson(mapOf("records" to db.daily().recent(365),"tasks" to db.tasks().all(),"prompts" to db.prompts().recent(1000)))}
 suspend fun importJson(json:String){withContext(Dispatchers.IO){val o=gson.fromJson(json,com.google.gson.JsonObject::class.java);o.getAsJsonArray("records")?.forEach{db.daily().upsert(gson.fromJson(it,DailyRecord::class.java))};o.getAsJsonArray("tasks")?.forEach{db.tasks().upsert(gson.fromJson(it,Task::class.java).copy(id=0))};o.getAsJsonArray("prompts")?.forEach{db.prompts().insert(gson.fromJson(it,PendingPrompt::class.java).copy(id=0))}}}

}
