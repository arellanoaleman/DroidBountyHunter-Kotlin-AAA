package edu.training.droidbountyhunterkotlin.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import edu.training.droidbountyhunterkotlin.R
import edu.training.droidbountyhunterkotlin.models.Fugitivo
import edu.training.droidbountyhunterkotlin.utils.PictureTools

class FugitivoAdapter (val context: Context, val listItems: Array<Fugitivo>) : BaseAdapter(){

    override fun getCount() = listItems.size

    override fun getItem(position: Int): Any = listItems[position]

    override fun getItemId(position: Int): Long = listItems[position].id.toLong()

    override fun getView(position: Int, view: View?, group: ViewGroup?): View {
        var myview = view

        val fugitivo = getItem(position) as Fugitivo

        if (myview == null){
            val inflater: LayoutInflater = LayoutInflater.from(context)
            myview = inflater.inflate(R.layout.item_fugitivo_list, null)
        }

        val textViewCaptura =  myview!!.findViewById<TextView>(R.id.txtFechaCaptura)
        val imageView = myview!!.findViewById<ImageView>(R.id.imagenFugitivo)
        val textViewNombre =  myview!!.findViewById<TextView>(R.id.textoRenglon)

        textViewNombre.text = fugitivo.name

        if (fugitivo!!.status == 1)
        {
            val bitmap = fugitivo!!.photo?.let{
                PictureTools.decodeSampledBitmapFromUri(it, 20, 20)
            }
            imageView!!.setImageBitmap(bitmap)

            textViewCaptura.text = "Capturado: ${fugitivo.date}"
        }
        else
        {
            textViewCaptura.text = "Sigue suelto..."
        }

        return myview
    }
}