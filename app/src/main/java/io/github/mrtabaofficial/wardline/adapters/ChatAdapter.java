package io.github.mrtabaofficial.wardline.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.mrtabaofficial.wardline.R;
import io.github.mrtabaofficial.wardline.databinding.ItemContainerReceivedMessageBinding;
import io.github.mrtabaofficial.wardline.databinding.ItemContainerSendMessageBinding;
import io.github.mrtabaofficial.wardline.models.ChatMessage;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    private Bitmap receiverProfileImage;
    private final String senderId;
    private OnMessageClickListener onMessageClickListener;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public interface OnMessageClickListener {
        void onDeleteMessage(ChatMessage chatMessage);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.hasSameContent(newItem);
        }
    };

    public ChatAdapter(Bitmap receiverProfileImage, String senderId) {
        super(DIFF_CALLBACK);
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.onMessageClickListener = listener;
    }

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(ItemContainerSendMessageBinding.inflate(inflater, parent, false));
        } else {
            return new ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(getItem(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(getItem(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).senderId.equals(senderId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSendMessageBinding binding;

        SentMessageViewHolder(ItemContainerSendMessageBinding itemContainerSendMessageBinding) {
            super(itemContainerSendMessageBinding.getRoot());
            binding = itemContainerSendMessageBinding;
            binding.getRoot().setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || getItem(position).deleted) {
                    return false;
                }
                showPopupMenu(v, getItem(position));
                return true;
            });
        }

        void setData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setAlpha(chatMessage.deleted ? 0.6f : 1f);
            binding.textDateTime.setText(chatMessage.dateTime);
        }

        private void showPopupMenu(View view, ChatMessage chatMessage) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_message_options, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete && onMessageClickListener != null) {
                    onMessageClickListener.onDeleteMessage(chatMessage);
                    return true;
                }
                return false;
            });
            popupMenu.show();
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setAlpha(chatMessage.deleted ? 0.6f : 1f);
            binding.textDateTime.setText(chatMessage.dateTime);
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}
