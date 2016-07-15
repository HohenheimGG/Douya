/*
 * Copyright (c) 2016 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.douya.broadcast.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.zhanghai.android.douya.broadcast.content.BroadcastCommentListResource;
import me.zhanghai.android.douya.broadcast.content.BroadcastResource;
import me.zhanghai.android.douya.broadcast.content.CommentListResource;
import me.zhanghai.android.douya.content.ResourceFragment;
import me.zhanghai.android.douya.network.api.info.Broadcast;
import me.zhanghai.android.douya.network.api.info.Comment;
import me.zhanghai.android.douya.util.FragmentUtils;

public class BroadcastAndCommentListResource extends ResourceFragment
        implements BroadcastResource.Listener, BroadcastCommentListResource.Listener {

    private static final String KEY_PREFIX = BroadcastAndCommentListResource.class.getName() + '.';

    public static final String EXTRA_BROADCAST_ID = KEY_PREFIX + "broadcast_id";
    public static final String EXTRA_BROADCAST = KEY_PREFIX + "broadcast";

    private long mBroadcastId;
    private Broadcast mBroadcast;

    private BroadcastResource mBroadcastResource;
    private CommentListResource mCommentListResource;

    private List<Runnable> mPendingCallbacks = new ArrayList<>();

    private static final String FRAGMENT_TAG_DEFAULT =
            BroadcastAndCommentListResource.class.getName();

    private static BroadcastAndCommentListResource newInstance(long broadcastId,
                                                               Broadcast broadcast) {
        //noinspection deprecation
        BroadcastAndCommentListResource resource = new BroadcastAndCommentListResource();
        resource.setArguments(broadcastId, broadcast);
        return resource;
    }

    public static BroadcastAndCommentListResource attachTo(long broadcastId, Broadcast broadcast,
                                                           FragmentActivity activity, String tag,
                                                           int requestCode) {
        return attachTo(broadcastId, broadcast, activity, tag, true, null, requestCode);
    }

    public static BroadcastAndCommentListResource attachTo(long broadcastId, Broadcast broadcast,
                                                           FragmentActivity activity) {
        return attachTo(broadcastId, broadcast, activity, FRAGMENT_TAG_DEFAULT,
                REQUEST_CODE_INVALID);
    }

    public static BroadcastAndCommentListResource attachTo(long broadcastId, Broadcast broadcast,
                                                           Fragment fragment, String tag,
                                                           int requestCode) {
        return attachTo(broadcastId, broadcast, fragment.getActivity(), tag, false, fragment,
                requestCode);
    }

    public static BroadcastAndCommentListResource attachTo(long broadcastId, Broadcast broadcast,
                                                           Fragment fragment) {
        return attachTo(broadcastId, broadcast, fragment, FRAGMENT_TAG_DEFAULT,
                REQUEST_CODE_INVALID);
    }

    private static BroadcastAndCommentListResource attachTo(long broadcastId, Broadcast broadcast,
                                                            FragmentActivity activity, String tag,
                                                            boolean targetAtActivity,
                                                            Fragment targetFragment,
                                                            int requestCode) {
        BroadcastAndCommentListResource resource = FragmentUtils.findByTag(activity, tag);
        if (resource == null) {
            resource = newInstance(broadcastId, broadcast);
            if (targetAtActivity) {
                resource.targetAtActivity(requestCode);
            } else {
                resource.targetAtFragment(targetFragment, requestCode);
            }
            FragmentUtils.add(resource, activity, tag);
        }
        return resource;
    }

    /**
     * @deprecated Use {@code attachTo()} instead.
     */
    public BroadcastAndCommentListResource() {}

    protected void setArguments(long broadcastId, Broadcast broadcast) {
        Bundle arguments = FragmentUtils.ensureArguments(this);
        arguments.putLong(EXTRA_BROADCAST_ID, broadcastId);
        arguments.putParcelable(EXTRA_BROADCAST, broadcast);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBroadcastAndIdFromArguments();

        // FIXME: Potential tag collision; we are not end user here.
        mBroadcastResource = BroadcastResource.attachTo(mBroadcastId, mBroadcast, this);
        mCommentListResource = BroadcastCommentListResource.attachTo(mBroadcastId, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getArguments().putParcelable(EXTRA_BROADCAST, mBroadcast);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mPendingCallbacks.clear();
    }

    public long getBroadcastId() {
        ensureBroadcastAndIdFromArguments();
        return mBroadcastId;
    }

    public Broadcast getBroadcast() {
        // Can be called before onCreate() is called.
        ensureBroadcastAndIdFromArguments();
        return mBroadcast;
    }

    public boolean hasBroadcast() {
        // Can be called before onCreate() is called.
        ensureBroadcastAndIdFromArguments();
        return mBroadcast != null;
    }

    public boolean isLoadingBroadcast() {
        return mBroadcastResource == null || mBroadcastResource.isLoading();
    }

    public List<Comment> getCommentList() {
        return mCommentListResource != null ? mCommentListResource.get() : null;
    }

    public boolean isCommentListEmpty() {
        return mCommentListResource == null || mCommentListResource.isEmpty();
    }

    public boolean isLoadingCommentList() {
        return mCommentListResource == null || mCommentListResource.isLoading();
    }

    public boolean isLoadingMoreCommentList() {
        return mCommentListResource != null && mCommentListResource.isLoadingMore();
    }

    private void ensureBroadcastAndIdFromArguments() {
        if (mBroadcast == null) {
            mBroadcast = getArguments().getParcelable(EXTRA_BROADCAST);
            // Be consistent with what the user will see first.
            mBroadcastId = mBroadcast != null ? mBroadcast.id
                    : getArguments().getLong(EXTRA_BROADCAST_ID);
        }
    }

    public void loadBroadcast() {
        if (mBroadcastResource != null) {
            mBroadcastResource.load();
        }
    }

    public void loadCommentList(boolean loadMore) {
        if (mCommentListResource != null) {
            mCommentListResource.load(loadMore);
        }
    }

    @Override
    public void onLoadBroadcastStarted(int requestCode) {
        getListener().onLoadBroadcastStarted(requestCode);
    }

    @Override
    public void onLoadBroadcastFinished(int requestCode) {
        getListener().onLoadBroadcastFinished(requestCode);
    }

    @Override
    public void onLoadBroadcastError(int requestCode, VolleyError error) {
        getListener().onLoadBroadcastError(requestCode, error);
    }

    @Override
    public void onBroadcastChanged(int requestCode, Broadcast newBroadcast) {
        mBroadcast = newBroadcast;
        getListener().onBroadcastChanged(requestCode, newBroadcast);
        Iterator<Runnable> iterator = mPendingCallbacks.iterator();
        while (iterator.hasNext()) {
            iterator.next().run();
            iterator.remove();
        }
    }

    @Override
    public void onBroadcastRemoved(int requestCode) {
        mBroadcast = null;
        getListener().onBroadcastRemoved(requestCode);
    }

    @Override
    public void onBroadcastWriteStarted(int requestCode) {
        getListener().onBroadcastWriteStarted(requestCode);
    }

    @Override
    public void onBroadcastWriteFinished(int requestCode) {
        getListener().onBroadcastWriteFinished(requestCode);
    }

    @Override
    public void onLoadCommentListStarted(final int requestCode) {
        if (hasBroadcast()) {
            getListener().onLoadCommentListStarted(requestCode);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onLoadCommentListStarted(requestCode);
                }
            });
        }
    }

    @Override
    public void onLoadCommentListFinished(final int requestCode) {
        if (hasBroadcast()) {
            getListener().onLoadCommentListFinished(requestCode);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onLoadCommentListFinished(requestCode);
                }
            });
        }
    }

    @Override
    public void onLoadCommentListError(final int requestCode, final VolleyError error) {
        if (hasBroadcast()) {
            getListener().onLoadCommentListError(requestCode, error);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onLoadCommentListError(requestCode, error);
                }
            });
        }
    }

    @Override
    public void onCommentListChanged(final int requestCode, List<Comment> newCommentList) {
        if (hasBroadcast()) {
            getListener().onCommentListChanged(requestCode, newCommentList);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onCommentListChanged(requestCode, mCommentListResource.get());
                }
            });
        }
    }

    @Override
    public void onCommentListAppended(final int requestCode, List<Comment> appendedCommentList) {
        if (hasBroadcast()) {
            getListener().onCommentListAppended(requestCode, appendedCommentList);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onCommentListChanged(requestCode, mCommentListResource.get());
                }
            });
        }
    }

    @Override
    public void onCommentRemoved(final int requestCode, int position) {
        if (hasBroadcast()) {
            getListener().onCommentRemoved(requestCode, position);
        } else {
            mPendingCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    getListener().onCommentListChanged(requestCode, mCommentListResource.get());
                }
            });
        }
    }

    private Listener getListener() {
        return (Listener) getTarget();
    }

    public interface Listener extends BroadcastResource.Listener,
            BroadcastCommentListResource.Listener {}
}
