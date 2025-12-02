package com.example.agribotz.app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.example.agribotz.R
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.ui.home.HomeActivity
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewmodels.main.LoginViewModelFactory
import com.example.agribotz.databinding.FragmentLoginBinding
import com.example.agribotz.app.viewmodels.main.LoginViewModel

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(Repository(), PreferencesManager(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupObservers()

        binding.forgotPasswordButton.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.passwordInputEditText.setOnEditorActionListener() {_, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    if (binding.passwordInputEditText.text?.isEmpty() == true) {
                        viewModel.setPasswordError(resources.getString(R.string.Required_Field))
                    }
                    else if (binding.mobileNumberInputEditText.text?.isEmpty() == true) {
                        viewModel.setMobileError(resources.getString(R.string.Required_Field))
                    }
                    else {
                        viewModel.onClickLogin()
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }

        return binding.root
    }

    private fun setupObservers() {
        viewModel.eventClickLogin.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                viewModel.onClickLoginCompleted()

                val intent = Intent(requireContext(), HomeActivity::class.java)
                startActivity(intent)

                requireActivity().finish()
            }
        }

        // Show event error messages
        viewModel.eventTransError.observe(viewLifecycleOwner) { errorResId ->
            errorResId?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }

        // Show dynamic server error messages
        viewModel.errorServerMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // Show Res server error messages
        viewModel.errorServerMessageRes.observe(viewLifecycleOwner) { resId ->
            resId?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.Forgot_Password_Title))
        builder.setMessage(getString(R.string.Forgot_Password_Text))
        builder.setPositiveButton(R.string.Ok) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
