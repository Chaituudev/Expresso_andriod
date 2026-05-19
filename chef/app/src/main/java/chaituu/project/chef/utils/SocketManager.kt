package chaituu.project.chef.utils

import android.util.Log
import chaituu.project.chef.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter

class SocketManager private constructor() {
    private var socket: Socket? = null
    private var listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    fun connect(cafeId: String) {
        if (socket?.connected() == true) {
            return
        }

        try {
            val options = IO.Options()
            options.transports = arrayOf("websocket")
            options.reconnection = true
            options.reconnectionDelay = 1000
            options.reconnectionDelayMax = 5000
            options.reconnectionAttempts = Integer.MAX_VALUE

            socket = IO.socket(BuildConfig.BACKEND_SOCKET_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Connected to socket server")
                socket?.emit("join-cafe", cafeId)
            }

            socket?.on("order-updated") { args ->
                Log.d("SocketManager", "Order updated: ${args[0]}")
                notifyListeners("order-updated", args.getOrNull(0))
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketManager", "Disconnected from socket server")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketManager", "Connection error: ${args[0]}")
            }

            socket?.connect()
        } catch (e: Exception) {
            Log.e("SocketManager", "Socket connection error", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun on(event: String, callback: (Any?) -> Unit) {
        if (!listeners.containsKey(event)) {
            listeners[event] = mutableListOf()
        }
        listeners[event]?.add(callback)
    }

    private fun notifyListeners(event: String, data: Any?) {
        listeners[event]?.forEach { callback ->
            callback(data)
        }
    }

    companion object {
        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(): SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager().also { instance = it }
            }
        }
    }
}
