package com.example.pawsociety

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddPetActivity : AppCompatActivity() {

    private lateinit var flImagePicker: FrameLayout
    private lateinit var ivPetImage: ImageView
    private lateinit var tvImagePlaceholder: TextView
    private lateinit var etPetName: EditText
    private lateinit var etPetBreed: EditText
    private lateinit var etPetAge: EditText
    private lateinit var btnSavePet: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private val petRepository = PetRepository()

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val validation = FileValidator.validateImage(this, it)
            if (validation is Resource.Success) {
                selectedImageUri = it
                ivPetImage.setImageURI(it)
                tvImagePlaceholder.visibility = View.GONE
            } else if (validation is Resource.Error) {
                Toast.makeText(this, validation.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)

        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }

        flImagePicker = findViewById(R.id.fl_image_picker)
        ivPetImage = findViewById(R.id.iv_pet_image)
        tvImagePlaceholder = findViewById(R.id.tv_image_placeholder)
        etPetName = findViewById(R.id.et_pet_name)
        etPetBreed = findViewById(R.id.et_pet_breed)
        etPetAge = findViewById(R.id.et_pet_age)
        btnSavePet = findViewById(R.id.btn_save_pet)
        progressBar = findViewById(R.id.progress_bar)

        flImagePicker.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnSavePet.setOnClickListener {
            savePet()
        }
    }

    private fun savePet() {
        val name = etPetName.text.toString().trim()
        val breed = etPetBreed.text.toString().trim()
        val age = etPetAge.text.toString().trim()

        if (name.isEmpty() || breed.isEmpty() || age.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val pet = Pet(
            name = name,
            breed = breed,
            age = age
        )

        progressBar.visibility = View.VISIBLE
        btnSavePet.isEnabled = false

        lifecycleScope.launch {
            val result = petRepository.createPet(this@AddPetActivity, pet, selectedImageUri)
            progressBar.visibility = View.GONE
            btnSavePet.isEnabled = true

            when (result) {
                is Resource.Success -> {
                    Toast.makeText(this@AddPetActivity, "Pet registered successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this@AddPetActivity, result.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}
