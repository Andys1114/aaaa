package com.example.notificationassistant

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

class NotificationMonitorService : NotificationListenerService() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val notifications = mutableListOf<NotificationData>()
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val title = notification.extras.getString("android.title") ?: ""
        val text = notification.extras.getString("android.text") ?: ""
        
        notifications.add(NotificationData(title, text))
        
        if (notifications.size >= 5) {
            coroutineScope.launch {
                summarizeNotifications()
                notifications.clear()
            }
        }
    }
    
    private suspend fun summarizeNotifications() {
        val apiService = Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
            
        val prompt = buildString {
            append("请整理和总结以下通知信息：\n\n")
            notifications.forEach { notification ->
                append("标题：${notification.title}\n")
                append("内容：${notification.text}\n\n")
            }
        }
        
        try {
            val response = apiService.generateSummary(
                "YOUR_API_KEY_HERE",
                ChatRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        Message(
                            role = "user",
                            content = prompt
                        )
                    )
                )
            )
            
            // 这里可以通过广播或其他方式将摘要发送到UI
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class NotificationData(
    val title: String,
    val text: String
)

interface OpenAIService {
    @POST("chat/completions")
    suspend fun generateSummary(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
) 