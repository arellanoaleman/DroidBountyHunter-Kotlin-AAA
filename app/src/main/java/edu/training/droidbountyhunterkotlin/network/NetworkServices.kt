package edu.training.droidbountyhunterkotlin.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

const val TAG = "CursoKotlin"
class NetworkServices {


    companion object{
        private const val ENDPOINT_FUGITIVOS ="http://3.13.226.218/droidBHServices.svc/fugitivos"
        private const val ENDPOINT_ATRAPADOS = "http://3.13.226.218/droidBHServices.svc/atrapados"
        private var JSONStr: String = ""
        private var tipo: SERVICE_TYPE = SERVICE_TYPE.FUGITIVOS
        private var code: Int = 0
        private var message: String = ""
        private var error: String = ""

        suspend fun execute(param: String?, listener: OnTaskListener, uuid: String? = null) {
            val result = withContext(Dispatchers.IO) {
                execute(param, uuid)
            }

            if (result) {
                listener.completedTask(JSONStr)
            } else {
                listener.errorTask(code, message, error)
            }
        }

        private fun execute(param: String?, uuid: String? = null) : Boolean{
            val esFugitivo = param.equals("Fugitivos", true)
            tipo = if (esFugitivo) SERVICE_TYPE.FUGITIVOS else SERVICE_TYPE.ATRAPADOS
            var urlConnection: HttpURLConnection? = null

            try
            {
                val url = if (esFugitivo) ENDPOINT_FUGITIVOS else ENDPOINT_ATRAPADOS
                urlConnection = getStructuredRequest(
                    tipo,
                    url,
                    uuid ?: ""
                )
                Log.d(TAG, "=> URL1: $url")

                val inputStream = urlConnection?.inputStream ?: return false
                val reader = BufferedReader(InputStreamReader(inputStream))
                val buffer = StringBuffer()

                do
                {
                    val line: String? = reader.readLine()
                    if (line != null)
                        buffer.append(line).append("\n")
                } while (line != null)

                if (buffer.isEmpty())
                    return false

                JSONStr = buffer.toString()
                Log.d(TAG, "Respuesta del servidor: $JSONStr")

                return true;
            }
            catch (e : FileNotFoundException)
            {
                manageError(urlConnection)
                return false;
            }
            catch (e : IOException)
            {
                manageError(urlConnection)
                return false;
            }
            catch (e : Exception)
            {
                manageError(urlConnection)
                return false;
            }
            finally
            {
                urlConnection?.disconnect()
            }
        }

        @Throws(IOException::class, JSONException::class)
        private fun getStructuredRequest(type: SERVICE_TYPE, endpoint: String, id: String): HttpURLConnection
        {
            val TIME_OUT = 5000
            val urlConnection: HttpURLConnection
            val url: URL?
            url = URL(endpoint)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.readTimeout = TIME_OUT
            urlConnection.setRequestProperty("Content-Type", "application/json")

            if (type == SERVICE_TYPE.FUGITIVOS)
            {
                //-------- GET Fugitivos ----------
                urlConnection.requestMethod = "GET"
                urlConnection.connect()
            }
            else
            {
                //--------------------- POST Atrapados ------------------------
                urlConnection.requestMethod = "POST"
                urlConnection.doInput = true
                urlConnection.doOutput = true
                urlConnection.connect()

                val objectJSON = JSONObject()
                objectJSON.put("UDIDString", id)

                val dataOutputStream = DataOutputStream(urlConnection.outputStream)
                dataOutputStream.write(objectJSON.toString().toByteArray())
                dataOutputStream.flush()
                dataOutputStream.close()
            }
            return urlConnection
        }

        private fun manageError(urlConnection: HttpURLConnection?) {
            if (urlConnection != null) {
                try {
                    code = urlConnection.responseCode
                    if (urlConnection.errorStream != null) {
                        val inputStream = urlConnection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val buffer = StringBuffer()
                        do {
                            val line: String? = reader.readLine()
                            if (line != null) buffer.append(line).append("\n")
                        } while (line != null)
                        error = buffer.toString()
                    } else {
                        message = urlConnection.responseMessage
                    }
                    error = urlConnection.errorStream.toString()
                    Log.e(TAG, "Error: $error, code: $code")
                } catch (e1: IOException) {
                    e1.printStackTrace()
                    Log.e(TAG, "Error")
                }
            } else {
                code = 105
                message = "Error: No internet connection"
                Log.e(TAG, "code: $code, $message")
            }
        }
    }

}

enum class SERVICE_TYPE{
    FUGITIVOS, ATRAPADOS
}

interface OnTaskListener{
    fun completedTask (response: String)
    fun errorTask(code: Int, message: String, error: String)
}