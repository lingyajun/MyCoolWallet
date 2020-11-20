package com.bethel.mycoolwallet.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Utils;
import com.xuexiang.xui.widget.toast.XToast;

import org.bitcoinj.core.Peer;
//import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VersionMessage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeerListAdapter extends ListAdapter<PeerListAdapter.ListItem, PeerListAdapter.MViewHolder> {
    private final LayoutInflater inflater;

    public PeerListAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ListItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                return Utils.equals(oldItem.ip, newItem.ip);
            }

            @Override
            public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
                if (!TextUtils.equals(oldItem.hostname, newItem.hostname)) return false;
                if (!TextUtils.equals(oldItem.ping, newItem.ping)) return false;
                if (oldItem.isDownloading != newItem.isDownloading)  return false;
                return true;
            }
        });

        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.peer_list_row, parent, false);
        return new MViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MViewHolder holder, int position) {
        final ListItem item = getItem(position);
        holder.ipView.setText(!TextUtils.isEmpty(item.hostname)? item.hostname: item.ip.getHostAddress());
        holder.heightView.setText(item.height>0 ? item.height + " blocks" : null);
        holder.versionView.setText(item.version);
        holder.protocolView.setText(item.protocol);
        holder.servicesView.setText(item.services);
        holder.pingView.setText(item.ping);

        Typeface tf = item.isDownloading? Typeface.DEFAULT_BOLD: Typeface.DEFAULT;
        holder.heightView.setTypeface(tf);
        holder.versionView.setTypeface(tf);
        holder.protocolView.setTypeface(tf);
        holder.servicesView.setTypeface(tf);
        holder.pingView.setTypeface(tf);

//        holder.ipView.setOnClickListener(view -> XToast.info(view.getContext(), item.peer.toString()).show());
    }

    public static List<ListItem> buildListItems(final Context context, final List<Peer> peers,
                                                final Map<InetAddress, String> map) {
        final List<ListItem> items = new ArrayList<>(peers.size());
        for (final Peer peer : peers)
            items.add(new ListItem(context, peer, map));
        return items;
    }

    public static class ListItem {
        public final InetAddress ip;
        public final String hostname;
        public final long height;
        public final String version;
        public final String protocol;
        public final String services;
        public final String ping;
        public final boolean isDownloading;

//        private final Peer peer; // test

        public ListItem(Context context, Peer peer,  Map<InetAddress, String> map) {
            ip= peer.getAddress().getAddr();
            hostname = map.get(ip);
            height = peer.getBestHeight();
            VersionMessage versionMessage = peer.getPeerVersionMessage();
            version = versionMessage.subVer;
            protocol = "protocol: " +versionMessage.clientVersion;
            services = peer.toStringServices(versionMessage.localServices);

            long pingTime = peer.getPingTime();
            ping = Long.MAX_VALUE > pingTime ?
                    context.getString(R.string.peer_list_row_ping_time, pingTime):null;
            isDownloading = peer.isDownloadData();

//            this.peer = peer;
        }
    }

    public static class MViewHolder extends RecyclerView.ViewHolder {
        private final TextView ipView;
        private final TextView heightView;
        private final TextView versionView;
        private final TextView protocolView;
        private final TextView servicesView;
        private final TextView pingView;

        public MViewHolder(@NonNull View itemView) {
            super(itemView);
            ipView =  itemView.findViewById(R.id.peer_list_row_ip);
            heightView =  itemView.findViewById(R.id.peer_list_row_height);
            versionView =  itemView.findViewById(R.id.peer_list_row_version);
            protocolView = itemView.findViewById(R.id.peer_list_row_protocol);
            servicesView =  itemView.findViewById(R.id.peer_list_row_services);
            pingView = itemView.findViewById(R.id.peer_list_row_ping);
        }
    }
}
