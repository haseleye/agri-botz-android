package com.example.agribotz.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.agribotz.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.net.toUri
import com.example.agribotz.databinding.FragmentGadgetLocationBinding
import com.google.android.material.appbar.MaterialToolbar

class GadgetLocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentGadgetLocationBinding? = null
    private val binding get() = _binding!!
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var gadgetName = ""
    private var googleMap: GoogleMap? = null
    private var isSatellite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lat = requireArguments().getFloat("lat").toDouble()
        lng = requireArguments().getFloat("lng").toDouble()
        gadgetName = requireArguments().getString("gadgetName", "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGadgetLocationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        val toolbar = binding.googleToolbar

        // Toolbar Back navigation
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Open external Google Maps
        val openExternalBtn = binding.btnOpenExternalMap

        openExternalBtn.setOnClickListener {
            openInGoogleMaps()
        }

        // Menu actions
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_satellite -> {
                    toggleMapType(toolbar)
                    true
                }
                else -> false
            }
        }

        // Map fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val location = LatLng(lat, lng)

        val marker = map.addMarker(
            MarkerOptions()
                .position(location)
                .title(gadgetName)
        )
        marker?.showInfoWindow()

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
    }

    private fun openInGoogleMaps() {
        val uri = "geo:$lat,$lng?q=$lat,$lng".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }

        // Fallback to browser if Google Maps app not installed
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun toggleMapType(toolbar: MaterialToolbar) {
        googleMap?.let { map ->
            isSatellite = !isSatellite

            val menuItem = toolbar.menu.findItem(R.id.action_toggle_satellite)

            if (isSatellite) {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                menuItem?.setIcon(R.drawable.ic_map)
                menuItem?.title = getString(R.string.Map_View)
            } else {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                menuItem?.setIcon(R.drawable.ic_satellite)
                menuItem?.title = getString(R.string.Satellite_View)
            }
        }
    }


}
