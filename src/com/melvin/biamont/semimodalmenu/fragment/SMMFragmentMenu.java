package com.melvin.biamont.semimodalmenu.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melvin.biamont.semimodalmenu.activity.SMMFragmentActivity;
import com.melvin.biamont.semimodalmenulib.R;

/**
 * Fragment used to create a SMM.
 * 
 * @author Melvin Biamont
 * 
 * @param <T>
 *            Activity in which the SMM will be integrated
 */
public class SMMFragmentMenu<T extends SMMFragmentActivity> extends Fragment {
	private static final String TAG = "SMMFragmentMenu";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.layout_semi_modal_menu_sample, null);
	}

	/**
	 * Look in the SMM (and only in the SMM) for a child view with the given id.
	 * If this view has the given id, return this view.
	 * 
	 * @param id
	 *            The id to search for.
	 * 
	 * @return The view that has the given id in the hierarchy or null
	 */
	public View findViewById(int id) {
		return getView().findViewById(id);
	}

	/**
	 * Open the SMM.
	 */
	public void openSMM() {
		getSMMActivity().openSMM();
	}

	/**
	 * Close the SMM.
	 */
	public void closeSMM() {
		getSMMActivity().closeSMM();
	}

	/**
	 * @return Activity (extending SMMFragmentActivity) in which the SMM has
	 *         been integrated.
	 */
	@SuppressWarnings("unchecked")
	public T getSMMActivity() {
		return (T) getActivity();
	}

	/**
	 * Called when the SMM is opening.
	 */
	public void onSMMOpening() {
		if (getSMMActivity().shouldLogEvent())
			Log.v(TAG, "internal : onSMMOpening();");
	}

	/**
	 * Called when the SMM is opened.
	 */
	public void onSMMOpened() {
		if (getSMMActivity().shouldLogEvent())
			Log.v(TAG, "internal : onSMMOpened();");
	}

	/**
	 * Called when the SMM is closing.
	 */
	public void onSMMClosing() {
		if (getSMMActivity().shouldLogEvent())
			Log.v(TAG, "internal : onSMMClosing();");
	}

	/**
	 * Called when the SMM is closed.
	 */
	public void onSMMClosed() {
		if (getSMMActivity().shouldLogEvent())
			Log.v(TAG, "internal : onSMMClosed();");
	}

	/**
	 * Called when the SMM is animating.
	 */
	public void onSMMAnimating() {
		if (getSMMActivity().shouldLogEvent())
			Log.v(TAG, "internal : onSMMAnimating();");
	}
}
