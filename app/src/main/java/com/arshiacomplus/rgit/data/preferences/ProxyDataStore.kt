package com.arshiacomplus.rgit.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "proxy_settings")

class ProxyDataStore(private val context: Context) {
    companion object {
        val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val PROXY_IP = stringPreferencesKey("proxy_ip")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val REPO_URL = stringPreferencesKey("repo_url")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
    }

    val proxySettingsFlow: Flow<AppConfig> = context.dataStore.data
        .map { preferences ->
            AppConfig(
                isEnabled = preferences[PROXY_ENABLED] ?: false,
                ip = preferences[PROXY_IP] ?: "127.0.0.1",
                port = preferences[PROXY_PORT] ?: 10808,
                threads = preferences[THREAD_COUNT] ?: 4,
                repoUrl = preferences[REPO_URL] ?: "",
                githubToken = preferences[GITHUB_TOKEN] ?: ""
            )
        }

    suspend fun saveProxyConfig(isEnabled: Boolean, ip: String, port: Int, threads: Int, repoUrl: String, githubToken: String) {
        context.dataStore.edit { preferences ->
            preferences[PROXY_ENABLED] = isEnabled
            preferences[PROXY_IP] = ip
            preferences[PROXY_PORT] = port
            preferences[THREAD_COUNT] = threads
            preferences[REPO_URL] = repoUrl
            preferences[GITHUB_TOKEN] = githubToken
        }
    }
}

data class AppConfig(val isEnabled: Boolean, val ip: String, val port: Int, val threads: Int, val repoUrl: String, val githubToken: String)