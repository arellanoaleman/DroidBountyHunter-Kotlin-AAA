package edu.training.droidbountyhunterkotlin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import edu.training.droidbountyhunterkotlin.data.DatabaseBountyHunter
import edu.training.droidbountyhunterkotlin.models.Fugitivo
import edu.training.droidbountyhunterkotlin.network.NetworkServices
import edu.training.droidbountyhunterkotlin.network.OnTaskListener
import edu.training.droidbountyhunterkotlin.utils.PictureTools
import edu.training.droidbountyhunterkotlin.utils.PictureTools.Companion.MEDIA_TYPE_IMAGE
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date

class DetalleActivity : AppCompatActivity(){

    var fugitivo: Fugitivo? = null
    var database: DatabaseBountyHunter? = null
    private var UDID: String? = ""

    private var direccionImagen: Uri? = null
    private var pictureFugitive: ImageView? = null

    private val REQUEST_CODE_GPS = 1234
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        setupLocationObjects()

        @SuppressLint("HardwareIds")
        UDID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        fugitivo = intent.extras?.get("fugitivo") as Fugitivo
        // Se obtiene el nombre del fugitivo del intent y se usa como título
        title = fugitivo!!.name + " - " + fugitivo!!.id

        // Se identifica si es Fugitivo o capturado para el mensaje...
        val etiquetaMensaje = findViewById<TextView>(R.id.etiquetaMensaje)

        pictureFugitive = findViewById(R.id.pictureFugitivo)
        if (fugitivo!!.status == 0)
        {
            etiquetaMensaje.text = "El fugitivo sigue suelto..."
            enableGPS()
        }else
        {
            etiquetaMensaje.text = "Atrapado!!!"

            val bitmap = fugitivo!!.photo?.let{
                PictureTools.decodeSampledBitmapFromUri(it, 200, 200)
            }
            pictureFugitive!!.setImageBitmap(bitmap)
        }
    }

    override fun onStop() {
        super.onStop()
        disableGPS()
    }

    override fun onDestroy() {
        pictureFugitive?.setImageBitmap(null)
        System.gc()
        super.onDestroy()
    }

    fun capturarFugitivoPresionado(view: View){
        capturarFugitivo();
        //botonCapturar.visibility = View.GONE
        //botonEliminar.visibility = View.GONE

    }

    fun eliminarFugitivoPresionado(view: View){
        database = DatabaseBountyHunter(this)
        database!!.borrarFugitivo(fugitivo!!)
        setResult(0)
        finish()
    }

    fun onFotoClick(view: View){
        if (PictureTools.permissionReadMemmory(this)){
            obtenFotoDeCamara()
        }
    }

    fun onMapClick(view: View) {
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("fugitivo", fugitivo)
        startActivity(intent)
    }

    fun capturarFugitivo(){
        database = DatabaseBountyHunter(this)
        fugitivo!!.status = 1

        if (fugitivo!!.photo.isNullOrEmpty()){
            Toast.makeText(this, "La foto del fugitivo es requerida!!", Toast.LENGTH_LONG).show()
            return
        }
        fugitivo!!.date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        database!!.actualizarFugitivo(fugitivo!!)

        lifecycleScope.launch {
            NetworkServices.execute("Atrapar", object: OnTaskListener {
                override fun completedTask(response: String) {
                    val obj = JSONObject(response)
                    val mensaje = obj.optString("mensaje","")
                    mensajeDeCerrado(mensaje)
                }

                override fun errorTask(code: Int, message: String, error: String) {
                    Toast.makeText(this@DetalleActivity,
                        "Ocurrio un problema en la comunicación con el WebService!!!",
                        Toast.LENGTH_LONG).show()
                }
            }, UDID)
        }
    }

    fun mensajeDeCerrado(mensaje: String){
        val builder = AlertDialog.Builder(this)
        builder.create()
        builder.setTitle("Alerta!!!")
            .setMessage(mensaje)
            .setOnDismissListener {
                setResult(fugitivo!!.status)
                finish()
            }.show()
    }

    private fun obtenFotoDeCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        direccionImagen = PictureTools.getOutputMediaFileUri(this, MEDIA_TYPE_IMAGE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, direccionImagen)
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
        if (it.resultCode == Activity.RESULT_OK) {
            fugitivo!!.photo = PictureTools.currentPhotoPath
            val bitmap = PictureTools.decodeSampledBitmapFromUri(PictureTools.currentPhotoPath, 200, 200)
            pictureFugitive?.setImageBitmap(bitmap)
        }
    }

    private fun setupLocationObjects() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult?.lastLocation != null) {
                    val location = locationResult.lastLocation
                    fugitivo!!.latitude = location!!.latitude
                    fugitivo!!.longitude = location!!.longitude
                } else {
                    Log.d("LocationCallback", "Location missing in callback.")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PictureTools.REQUEST_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            {
                Log.d("RequestPermissions", "Camera - Granted")
                obtenFotoDeCamara()
            } else {
                Log.d("RequestPermissions", "Camera - Not Granted")
            }
        }
        else if (requestCode == REQUEST_CODE_GPS)
        {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                enableGPS()
            }
            else
            {
                Log.d("RequestPermissions", "GPS - Not Granted")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableGPS(){
        if (isGPSActivated())
        {
            fusedLocationClient?.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper())
            Toast.makeText(this, "Activando GPS...", Toast.LENGTH_LONG).show()

            fusedLocationClient?.lastLocation?.addOnSuccessListener { location : Location? ->

                location?.let {
                    fugitivo!!.latitude = location.latitude
                    fugitivo!!.longitude = location.longitude
                }
            }
        }
    }

    private fun disableGPS()
    {
        Toast.makeText(this, "Desactivando GPS...", Toast.LENGTH_LONG).show()

        val removeTask = fusedLocationClient?.removeLocationUpdates(locationCallback!!)
        removeTask?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("LocationRequest", "Location Callback removed.")
            } else {
                Log.d("LocationRequest", "Failed to remove Location Callback.")
            }
        }
    }

    private fun isGPSActivated(): Boolean {
       return if ( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
       {
           if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
               ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_GPS)
               false
           }else{
               ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_GPS)
               false
           }
       }else{
           true
       }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detalle, menu)
        if (fugitivo!!.status == 1)
        {
            menu.findItem(R.id.menu_detalle_foto).setVisible(false);
            menu.findItem(R.id.menu_detalle_capturar).setVisible(false);
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_detalle_capturar -> {
                capturarFugitivo()
                true
            }
            R.id.menu_detalle_eliminar -> {
                database = DatabaseBountyHunter(this)
                database!!.borrarFugitivo(fugitivo!!)
                setResult(0)
                finish()
                true
            }
            R.id.menu_detalle_foto -> {
                if (PictureTools.permissionReadMemmory(this)){
                    obtenFotoDeCamara()
                }
                true
            }
            R.id.menu_detalle_mapa -> {
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("fugitivo", fugitivo)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}