package com.example.todolist.ui.topics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.databinding.FragmentTopicsBinding;

public class TopicsFragment extends Fragment {
    private FragmentTopicsBinding binding;
    private TopicsViewModel viewModel;
    private TopicAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTopicsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(TopicsViewModel.class);
        adapter = new TopicAdapter(topic ->
            AddEditTopicBottomSheet.newInstance(topic).show(getChildFragmentManager(), "edit_topic"));

        binding.rvTopics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTopics.setAdapter(adapter);

        viewModel.getTopics().observe(getViewLifecycleOwner(), topics -> {
            adapter.submitList(topics);
            binding.count.setText(topics.size() + " topics");
        });

        binding.fabAddTopic.setOnClickListener(v ->
            AddEditTopicBottomSheet.newInstance(null).show(getChildFragmentManager(), "add_topic"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
