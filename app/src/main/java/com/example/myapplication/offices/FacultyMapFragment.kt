package com.example.myapplication.offices

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import java.io.BufferedReader
import java.io.InputStreamReader

class FacultyMapFragment : Fragment() {

    private lateinit var searchView: SearchView
    private val allOfficeTextViews = mutableListOf<TextView>()
    private val officeData = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_faculty_map, container, false)

        searchView = view.findViewById(R.id.teacher_search_view)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val headerContainer = view.findViewById<LinearLayout>(R.id.header_container)

        // 1. Back Button Logic
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Handle Window Insets (Apply margin to the whole header container)
        val originalMargin = (16 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Push the whole top bar (Back Button + Search) down
            val params = headerContainer.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = originalMargin + insets.top
            headerContainer.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        // 3. Load Data
        loadDataFromCsv()
        populateMap(view)
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
        for ((roomId, teacherName) in officeData) {
            val resourceId = resources.getIdentifier(roomId, "id", requireContext().packageName)
            if (resourceId != 0) {
                val textView = rootView.findViewById<TextView>(resourceId)
                if (textView != null) {
                    textView.text = teacherName
                    allOfficeTextViews.add(textView)
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
    override fun onDestroyView() {
        super.onDestroyView()
        // Force hide keyboard so it doesn't mess up the next screen
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}