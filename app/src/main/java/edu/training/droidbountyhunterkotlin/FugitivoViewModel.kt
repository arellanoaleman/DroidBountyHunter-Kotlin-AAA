package edu.training.droidbountyhunterkotlin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.training.droidbountyhunterkotlin.models.Fugitivo

class FugitivoViewModel : ViewModel() {
    private val mutableSelectedFugitivo = MutableLiveData<Fugitivo>()
    val selectedFugitivo: LiveData<Fugitivo> get() = mutableSelectedFugitivo

    fun selectFugitivo(fugitivo: Fugitivo) {
        mutableSelectedFugitivo.value = fugitivo
    }
}