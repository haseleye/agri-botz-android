package com.example.agribotz.app.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.agribotz.databinding.FragmentStarterBinding
import androidx.appcompat.app.AlertDialog
import com.example.agribotz.R

class StarterFragment : Fragment() {

    private var _binding: FragmentStarterBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = FragmentStarterBinding.inflate(inflater, container, false)

        binding.existingUserButton.setOnClickListener {
            val action = StarterFragmentDirections.actionStarterFragmentToLoginFragment()
            findNavController().navigate(action)
        }
        binding.enrollButton.setOnClickListener {
            showEnrollDialog()
        }

        return binding.root
    }

    private fun showEnrollDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.Enroll_Title))
        builder.setMessage(getString(R.string.Enroll_Text))
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