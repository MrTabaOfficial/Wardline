package io.github.mrtabaofficial.wardline.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.mrtabaofficial.wardline.databinding.ItemContainerRecentConversionBinding;
import io.github.mrtabaofficial.wardline.listeners.ConversionListener;
import io.github.mrtabaofficial.wardline.models.ChatMessage;
import io.github.mrtabaofficial.wardline.models.User;
import io.github.mrtabaofficial.wardline.utilities.ImageUtils;

public class RecentConversationsAdapter extends ListAdapter<ChatMessage, RecentConversationsAdapter.ConversionViewHolder> {
    private final ConversionListener conversionListener;

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.conversionId.equals(newItem.conversionId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.hasSameContent(newItem);
        }
    };

    public RecentConversationsAdapter(ConversionListener conversionListener) {
        super(DIFF_CALLBACK);
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(getItem(position));
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(ImageUtils.decode(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }
}
