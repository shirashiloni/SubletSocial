package com.example.subletsocial.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.subletsocial.R
import com.example.subletsocial.databinding.FragmentLoginBinding
import com.example.subletsocial.model.User
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isLoginMode = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (auth.currentUser != null) {
            goToFeed()
        }

        binding.tvToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false

            if (isLoginMode) {
                loginUser(email, password)
            } else {
                registerUser(email, password)
            }
        }
    }

    private fun updateUI() {
        if (isLoginMode) {
            binding.tilName.visibility = View.GONE
            binding.tilEmail.layoutParams = (binding.tilEmail.layoutParams as ConstraintLayout.LayoutParams).apply {
                topToBottom = binding.tvSubtitle.id
                topMargin = dpToPx(48)
            }

            binding.tvTitle.text = "Welcome Back"
            binding.btnLogin.text = "Login"
            binding.tvToggleMode.text = "Don't have an account? Sign up"
        } else {
            binding.tilName.visibility = View.VISIBLE
            binding.tilEmail.layoutParams = (binding.tilEmail.layoutParams as ConstraintLayout.LayoutParams).apply {
                topToBottom = binding.tilName.id
                topMargin = dpToPx(16)
            }

            binding.tvTitle.text = "Create Account"
            binding.btnLogin.text = "Sign Up"
            binding.tvToggleMode.text = "Already have an account? Login"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun loginUser(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    goToFeed()
                } else {
                    Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser(email: String, pass: String) {
        val name = binding.etName.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    val newUser = User(userId, name, email, "")

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(newUser)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            binding.btnLogin.isEnabled = true
                            goToFeed()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToFeed() {
        findNavController().navigate(R.id.action_loginFragment_to_feedFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}