package com.seventhelement.donttouchmyphone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.seventhelement.donttouchmyphone.Adapter.Adapter
import com.seventhelement.donttouchmyphone.databinding.BottomSheetSoundPickerBinding

class SoundPickerBottomSheet(
    private val initialSelection: Int,
    private val onSelected: (position: Int) -> Unit
) : BottomSheetDialogFragment(), Adapter.OnItemSelectedListener {

    private var _binding: BottomSheetSoundPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: Adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSoundPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = Adapter(
            Constants.getlist(),
            requireContext(),
            requireContext().packageName,
            this,
            initialSelection,
            true
        )
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.btnDone.setOnClickListener {
            adapter.releaseMediaPlayer()
            dismiss()
        }
    }

    override fun onItemSelected(position: Int) {
        onSelected(position)
    }

    override fun onItemDeselected() {}

    override fun onDestroyView() {
        adapter.releaseMediaPlayer()
        super.onDestroyView()
        _binding = null
    }
}
