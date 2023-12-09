package edu.training.droidbountyhunterkotlin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.fragment.app.Fragment
import edu.training.droidbountyhunterkotlin.R

class AcercaDeFragment : Fragment(){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
// Se hace referencia al Fragment generado por XML en los Layouts y
// se instancia en una View...
        return inflater.inflate(R.layout.fragment_acerca_de, container,false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var rating = "0.0" // Variable para lectura del rating guardado en Properties
        if (System.getProperty("rating") != null){
            rating = System.getProperty("rating")
        }
        if (rating.isEmpty()){
            rating = "0.0"
        }
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        ratingBar.rating = rating.toFloat()
        ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            System.setProperty("rating", rating.toString())
            ratingBar.rating = rating
        }
    }
}