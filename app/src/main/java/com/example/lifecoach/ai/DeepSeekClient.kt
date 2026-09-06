package com.example.lifecoach.ai

import com.example.lifecoach.data.CoachRepository
import com.google.gson.*
import java.net.HttpURLConnection
import java.net.URL

class DeepSeekClient(private val repo:CoachRepository){
 private val persona="你是这个人的私人教练。严厉、直接、说人话。每次不超过3句，不用列表和emoji。必须挂在具体数据上，批评行为不定义人，动作小到当场能做。禁止加油、相信自己、你可以的等套话。"
 fun ask(instruction:String,context:String=""):String { val key=repo.apiKey(); if(key.isBlank()) return fallback(instruction); return try { val body=JsonObject().apply{addProperty("model","deepseek-chat");add("messages",JsonArray().apply{add(JsonObject().apply{addProperty("role","system");addProperty("content","$persona\n$context")});add(JsonObject().apply{addProperty("role","user");addProperty("content",instruction)})});addProperty("temperature",1.0);addProperty("max_tokens",300);addProperty("stream",false)}; val c=(URL("https://api.deepseek.com/chat/completions").openConnection() as HttpURLConnection).apply{requestMethod="POST";setRequestProperty("Authorization","Bearer $key");setRequestProperty("Content-Type","application/json");connectTimeout=10000;readTimeout=20000;doOutput=true}; c.outputStream.use{it.write(body.toString().toByteArray())}; if(c.responseCode !in 200..299) return fallback(instruction); JsonParser.parseString(c.inputStream.bufferedReader().readText()).asJsonObject["choices"].asJsonArray[0].asJsonObject["message"].asJsonObject["content"].asString.trim().take(600)
 }catch(_:Exception){fallback(instruction)} }
 private fun fallback(i:String)=when{ i.contains("重排")||i.contains("三件事")->"今天只排三件：先做最重要的一件，再完成一个能收尾的小动作，晚上留出二十五分钟学习。";i.contains("报告")||i.contains("复盘")->"看数据说话。把今天没做的那件事明早第一时间补上。";i.contains("滑")||i.contains("成人")->"现在先离开屏幕，走到门口再决定。给自己十五分钟。";else->"先做一个十五分钟的小动作，做完再回来。"}
}
