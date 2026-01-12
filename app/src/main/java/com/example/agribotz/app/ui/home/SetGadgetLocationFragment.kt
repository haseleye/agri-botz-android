package com.example.agribotz.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.agribotz.R
import com.example.agribotz.app.repository.Repository
import com.example.agribotz.app.util.PreferencesManager
import com.example.agribotz.app.viewmodels.home.SetGadgetLocationViewModel
import com.example.agribotz.app.viewmodels.home.SetGadgetLocationViewModelFactory
import com.example.agribotz.databinding.FragmentSetGadgetLocationBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class SetGadgetLocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSetGadgetLocationBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selectedMarker: Marker? = null
    private var selectedLatLng: LatLng? = null

    // Fetching arguments
    private val gadgetId: String by lazy { arguments?.getString("gadgetId") ?: "" }
    private val hasExistingGps: Boolean by lazy {
        gadgetLat != 0f && gadgetLng != 0f
    }
    private val gadgetLat: Float by lazy {
        requireArguments().getFloat("gadgetLat")
    }
    private val gadgetLng: Float by lazy {
        requireArguments().getFloat("gadgetLng")
    }

    private val viewModel: SetGadgetLocationViewModel by viewModels {
        SetGadgetLocationViewModelFactory(
            Repository(),
            PreferencesManager(requireContext()),
            gadgetId
        )
    }

    /** Permission launcher */
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                requestCurrentLocationInternal()
            }
            else {
                Toast.makeText(
                    requireContext(),
                    R.string.Location_Permission_Required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSetGadgetLocationBinding.inflate(inflater, container, false)

        viewModel.showSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Snackbar.make(
                    requireView(),
                    R.string.Location_Updated,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.eventTransError.observe(viewLifecycleOwner) { errRes ->
            errRes?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show()
                viewModel.onTransErrorCompleted()
            }
        }

        viewModel.errorServerMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        viewModel.errorServerMessageRes.observe(viewLifecycleOwner) { res ->
            res?.let { Toast.makeText(requireContext(), getString(it), Toast.LENGTH_LONG).show() }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        binding.setLocationToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        viewModel.done.observe(viewLifecycleOwner) { done ->
            if (done == true) {
                findNavController().popBackStack()
                viewModel.onNavigatedBack()
            }
        }

        // Current Location
        binding.btnUseCurrentLocation.setOnClickListener {
            binding.locationLoading.isVisible = true
            binding.btnUseCurrentLocation.isEnabled = false
            requestCurrentLocation()
        }

        // Confirm Button
        binding.btnConfirmLocation.setOnClickListener {
            viewModel.onConfirmLocation()
        }
        binding.btnConfirmLocation.isEnabled = false

        viewModel.selectedGps.observe(viewLifecycleOwner) {
            binding.btnConfirmLocation.isEnabled = it != null
        }

        // Map fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (hasExistingGps) {
            val latLng = LatLng(gadgetLat.toDouble(), gadgetLng.toDouble())

            // Hide center pin
            binding.centerPin.isVisible = false
            binding.locationHint.text = getString(R.string.Save_Or_Select_Another)

            // Add marker
            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.Selected_Location))
            )

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

            viewModel.onLocationSelected(
                latLng.latitude,
                latLng.longitude
            )
        }
        else {
            val defaultLocation = LatLng(30.0444, 31.2357)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        }

        map.setOnMapClickListener { latLng ->
            selectLocation(latLng)
        }
    }

    /** Permission check wrapper */
    private fun requestCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            requestCurrentLocationInternal()
        }
        else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocationInternal() {
        val fusedClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                binding.locationLoading.isVisible = false
                binding.btnUseCurrentLocation.isEnabled = true

                if (location != null) {
                    selectLocation(LatLng(location.latitude, location.longitude))
                } else {
                    Toast.makeText(
                        requireContext(),
                        R.string.Location_Not_Available,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                binding.locationLoading.isVisible = false
                binding.btnUseCurrentLocation.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    R.string.Location_Not_Available,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun selectLocation(latLng: LatLng) {
        selectedLatLng = latLng
        selectedMarker?.remove()
        binding.centerPin.isVisible = false
        binding.locationHint.text = getString(R.string.Save_Or_Select_Another)

        addMarkerWithDropAnimation(latLng)

        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        )

        viewModel.onLocationSelected(latLng.latitude, latLng.longitude)
    }

    private fun addMarkerWithDropAnimation(latLng: LatLng) {
        val map = googleMap ?: return

        // Offset start position slightly above
        val startLatLng = LatLng(
            latLng.latitude + 0.0005,
            latLng.longitude
        )

        val marker = map.addMarker(
            MarkerOptions()
                .position(startLatLng)
                .title(getString(R.string.Selected_Location))
        )

        marker?.let {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val startTime = System.currentTimeMillis()
            val duration = 600L // ms
            val interpolator = android.view.animation.BounceInterpolator()

            handler.post(object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - startTime
                    val t = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                    val v = interpolator.getInterpolation(t)

                    val newLat = startLatLng.latitude +
                            (latLng.latitude - startLatLng.latitude) * v

                    it.position = LatLng(newLat, latLng.longitude)

                    if (t < 1f) {
                        handler.postDelayed(this, 16)
                    }
                }
            })
        }

        selectedMarker = marker
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
