package com.example.myapplication.offices

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import java.io.BufferedReader
import java.io.InputStreamReader

class FacultyMapFragment : Fragment() {

    private lateinit var searchView: SearchView
    private val allOfficeTextViews = mutableListOf<TextView>()
    private val officeData = mutableMapOf<String, String>() // Holds room_id -> teacher_name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_faculty_map, container, false)

        val toolbar: Toolbar = view.findViewById(R.id.faculty_map_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.title = "Faculty Offices"
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        searchView = view.findViewById(R.id.teacher_search_view)

        // 1. Load the teacher data from your CSV file
        loadDataFromCsv()

        // 2. Find all your TextViews in the XML and fill them with the data
        populateMap(view)

        // 3. Set up the search functionality
        setupSearchView()

        return view
    }

    private fun loadDataFromCsv() {
        try {
            val inputStream = resources.openRawResource(R.raw.faculty_data)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size == 2) {
                    val roomId = tokens[0].trim()
                    val teacherName = tokens[1].trim().removeSurrounding("\"")
                    officeData[roomId] = teacherName
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("FacultyMapFragment", "Error reading CSV file", e)
        }
    }

    private fun populateMap(rootView: View) {
        // Loop through all the data we loaded from the CSV
        for ((roomId, teacherName) in officeData) {
            // Find the TextView in your layout that has the matching ID (e.g., "room_1")
            val resourceId = resources.getIdentifier(roomId, "id", requireContext().packageName)
            if (resourceId != 0) {
                val textView = rootView.findViewById<TextView>(resourceId)
                if (textView != null) {
                    textView.text = teacherName
                    allOfficeTextViews.add(textView) // Add to our list for searching
                }
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterMap(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMap(newText)
                return true
            }
        })
    }

    private fun filterMap(query: String?) {
        val searchText = query?.lowercase()?.trim() ?: ""

        for (textView in allOfficeTextViews) {
            val teacherName = textView.text.toString().lowercase()
            if (searchText.isNotEmpty() && teacherName.contains(searchText)) {
                textView.setBackgroundResource(R.drawable.cell_border_highlighted)
            } else {
                textView.setBackgroundResource(R.drawable.cell_border)
            }
        }
    }
}