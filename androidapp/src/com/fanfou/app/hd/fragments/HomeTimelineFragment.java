package com.fanfou.app.hd.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.fanfou.app.hd.App;
import com.fanfou.app.hd.api.Paging;
import com.fanfou.app.hd.dao.model.StatusModel;
import com.fanfou.app.hd.service.FanFouService;
import com.fanfou.app.hd.util.Utils;

/**
 * @author mcxiaoke
 * @version 1.0 2012.02.06
 * @version 1.1 2012.02.24
 * 
 */
public class HomeTimelineFragment extends BaseTimlineFragment {
	private static final String TAG = HomeTimelineFragment.class
			.getSimpleName();

	public static HomeTimelineFragment newInstance(int type) {
		Bundle args = new Bundle();
		args.putInt("type", type);
		HomeTimelineFragment fragment = new HomeTimelineFragment();
		fragment.setArguments(args);
		if (App.DEBUG) {
			Log.d(TAG, "newInstance() "+fragment);
		}
		return fragment;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
//		if (getAdapter().isEmpty()) {
//			startRefresh();
//		}
	}

	@Override
	protected int getType() {
		return StatusModel.TYPE_HOME;
	}

	@Override
	protected void doFetch(boolean doGetMore) {

		final ResultHandler handler = new ResultHandler(this);
		final Cursor cursor = getCursor();
		Paging p=new Paging();
		if (doGetMore) {
			p.maxId = Utils.getMaxId(cursor);
		} else {
			p.sinceId = Utils.getSinceId(cursor);
		}
		if (App.DEBUG) {
			Log.d(TAG, "doFetch() doGetMore=" + doGetMore+" Paging="+p);
		}
		FanFouService.getTimeline(getActivity(), StatusModel.TYPE_HOME, handler, p);
	}

}
