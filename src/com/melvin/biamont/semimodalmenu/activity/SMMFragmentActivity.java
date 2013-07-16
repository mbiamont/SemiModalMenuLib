package com.melvin.biamont.semimodalmenu.activity;

import java.util.ArrayList;
import java.util.Collection;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.melvin.biamont.semimodalmenu.fragment.SMMFragmentMenu;
import com.melvin.biamont.semimodalmenu.listener.SMMListeners.OnSMMAnimating;
import com.melvin.biamont.semimodalmenu.listener.SMMListeners.OnSMMClosed;
import com.melvin.biamont.semimodalmenu.listener.SMMListeners.OnSMMClosing;
import com.melvin.biamont.semimodalmenu.listener.SMMListeners.OnSMMOpened;
import com.melvin.biamont.semimodalmenu.listener.SMMListeners.OnSMMOpening;
import com.melvin.biamont.semimodalmenulib.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewPropertyAnimator;

/**
 * Activity providing a SMM (Semi Modal Menu) appearing on the bottom of the
 * screen. It uses a {@link SMMFragmentMenu} to manage the menu.
 * 
 * @author Melvin Biamont
 * 
 */
public abstract class SMMFragmentActivity extends FragmentActivity {
	private static final String TAG = "SMMFragmentActivity";
	private static final String FRAGMENT_TAG = "SMMFragment";

	/* Views used to manage the SMM */
	private ViewGroup mDecorView;
	private LinearLayout mSMMContainer;
	private View mContentView;
	private View mLayerView;

	/* The menu fragment */
	private SMMFragmentMenu<?> mSMMFragment;

	/* Some booleans used to know the state of the menu */
	private boolean isSMMAlreadyAttached = false;
	private boolean isSMMOpened = false;
	private boolean isSMMOpening = false;
	private boolean isSMMAnimating = false;
	private boolean wasSMMOpenedBeforeRotate = false;

	/* Some variables... */
	private int mDecorViewHeight;
	private int mStatusBarHeight;
	private int mMenuViewHeight;

	/* Listeners */
	private OnSMMOpening mOnSMMOpeningListener;
	private OnSMMOpened mOnSMMOpenedListener;
	private OnSMMClosing mOnSMMClosingListener;
	private OnSMMClosed mOnSMMClosedListener;
	private OnSMMAnimating mOnSMMAnimatingListener;

	/*
	 * METHODS OVERRIDING ACTIVITY
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isSMMOpened = false;
		isSMMOpening = false;
		isSMMAnimating = false;
		if (savedInstanceState != null) {
			mSMMFragment = (SMMFragmentMenu<?>) getSupportFragmentManager()
					.findFragmentByTag(FRAGMENT_TAG);
			if (savedInstanceState.getBoolean("isMenuOpened")) {
				wasSMMOpenedBeforeRotate = true;
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isMenuOpened", isSMMOpened);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onBackPressed() {
		if (shouldCloseTheSMMOnBackPressed()) {
			if (isSMMAnimating) {
				if (shouldLogEvent())
					Log.v(TAG,
							"internal : onBackPressed() called when the SMM was animating... Cancelling BACK press.");
				return;
			}
			if (isSMMOpened) {
				if (shouldLogEvent())
					Log.v(TAG,
							"internal : onBackPressed() called when the SMM was opened. It'll be closed.");
				closeSMM();
				return;
			}
		}
		super.onBackPressed();
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		checkDecorViewBeforeContentView();
		attachMenuViewToActivity();
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);
		checkDecorViewBeforeContentView();
		attachMenuViewToActivity();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		super.setContentView(view, params);
		checkDecorViewBeforeContentView();
		attachMenuViewToActivity();
	}

	/****************************************************************************************************************/

	private void checkDecorViewBeforeContentView() {
		if (mDecorView != null) {
			if (shouldLogEvent())
				Log.v(TAG, "internal : mDecorView not null.");
			removeMenuViewFromActivity();
		}
	}

	/****************************************************************************************************************/

	/*
	 * METHODS (PRIVATE) CREATING OR DESTRUCTING THE SMM
	 */

	/**
	 * Attachs the SMM's view on the activity.
	 */
	private void attachMenuViewToActivity() {
		mDecorView = ((ViewGroup) getWindow().getDecorView());
		mContentView = mDecorView.getChildAt(0);
		mDecorView.setBackgroundResource(getSMMWindowBackgroundResources());
		mSMMContainer = new LinearLayout(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.BOTTOM;
		mSMMContainer
				.addView(
						LayoutInflater.from(this).inflate(
								R.layout.smm_container, null), lp);
		mSMMContainer.setId(R.id.smm_container_id);

		// On ajoute le menu semi-modal à la fenêtre
		mSMMContainer.post(new Runnable() {
			public void run() {
				Rect rectgle = new Rect();
				getWindow().getDecorView()
						.getWindowVisibleDisplayFrame(rectgle);
				mStatusBarHeight = rectgle.top;
				mDecorViewHeight = getWindow().getDecorView().getHeight();
				mMenuViewHeight = mDecorViewHeight - mStatusBarHeight;
				mLayerView = new View(SMMFragmentActivity.this);
				mLayerView.setId(R.id.smm_layer_id);
				mLayerView.setBackgroundColor(Color.BLACK);
				mDecorView.addView(mLayerView, new ViewGroup.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, mMenuViewHeight);
				lp.gravity = Gravity.BOTTOM;
				mDecorView.addView(mSMMContainer, lp);

				mLayerView.setVisibility(View.GONE);
				mLayerView.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if (shouldPreventActivityFromTouch()
								&& shouldOnClickOnContentCloseSMMenu()
								&& !isSMMAnimating() && isSMMOpened()) {
							if (shouldLogEvent())
								Log.v(TAG,
										"internal : The SMM has prevented the Activity from touch.");
							closeSMM();
							return true;
						}
						return shouldPreventActivityFromTouch();
					}
				});
				ViewPropertyAnimator.animate(mSMMContainer).setDuration(0)
						.y(mDecorViewHeight);
				ViewPropertyAnimator.animate(mLayerView).setDuration(0)
						.alpha(0);

				if (mSMMFragment == null) {
					mSMMFragment = getSMMFragment();
					getSupportFragmentManager()
							.beginTransaction()
							.replace(R.id.smm_container, mSMMFragment,
									FRAGMENT_TAG).commit();
				} else {
					getSupportFragmentManager()
							.beginTransaction()
							.detach(mSMMFragment)
							.replace(R.id.smm_container, getSMMFragment(),
									FRAGMENT_TAG).commit();
					if (wasSMMOpenedBeforeRotate) {
						ObjectAnimator anim1 = ObjectAnimator.ofFloat(
								mContentView, "scaleX", 1f, 0.9f)
								.setDuration(0);
						ObjectAnimator anim2 = ObjectAnimator.ofFloat(
								mContentView, "scaleY", 1f, 0.8f)
								.setDuration(0);
						anim2.setStartDelay(0);
						ObjectAnimator anim3 = ObjectAnimator.ofFloat(
								mContentView, "y", -75f).setDuration(0);
						ObjectAnimator anim4 = ObjectAnimator.ofFloat(
								mContentView, "rotationX", 0f, 5f).setDuration(
								0);
						ObjectAnimator anim5 = ObjectAnimator.ofFloat(
								mContentView, "rotationX", 5f, 0f).setDuration(
								0);
						Collection<Animator> animators = new ArrayList<Animator>();
						animators.add(anim1);
						animators.add(anim2);
						animators.add(anim3);
						animators.add(anim4);
						animators.add(anim5);
						AnimatorSet animatorSet = new AnimatorSet();
						animatorSet.addListener(new AnimatorListener() {

							@Override
							public void onAnimationStart(Animator animation) {
								isSMMOpened = false;
								isSMMOpening = true;
							}

							@Override
							public void onAnimationRepeat(Animator animation) {
							}

							@Override
							public void onAnimationEnd(Animator animation) {
								isSMMAnimating = false;
								isSMMOpened = true;
								isSMMOpening = false;
							}

							@Override
							public void onAnimationCancel(Animator animation) {
							}
						});
						animatorSet.playTogether(animators);
						animatorSet.start();

						ViewPropertyAnimator.animate(mLayerView).setDuration(0)
								.setListener(new AnimatorListener() {

									@Override
									public void onAnimationStart(
											Animator animation) {
										mLayerView.setVisibility(View.VISIBLE);
									}

									@Override
									public void onAnimationRepeat(
											Animator animation) {
									}

									@Override
									public void onAnimationEnd(
											Animator animation) {
									}

									@Override
									public void onAnimationCancel(
											Animator animation) {
									}
								}).alpha(getSMMLayerOpacity());
						ViewPropertyAnimator.animate(mSMMContainer)
								.setDuration(0).y(mStatusBarHeight);
						wasSMMOpenedBeforeRotate = false;
					}
				}

				isSMMAlreadyAttached = true;
				if (shouldLogEvent())
					Log.v(TAG,
							"internal : attachMenuViewToActivity() executed.");
			}
		});
	}

	/**
	 * Removes the SMM's view from the activity.
	 */
	private void removeMenuViewFromActivity() {
		mSMMContainer.post(new Runnable() {

			@Override
			public void run() {
				getSupportFragmentManager().beginTransaction()
						.remove(mSMMFragment).commit();
				mDecorView.removeView(mSMMContainer);
				mDecorView.removeView(mLayerView);
				if (shouldLogEvent())
					Log.v(TAG,
							"internal : removeMenuViewFromActivity() executed.");
			}
		});
	}

	/*
	 * METHODS PERFORMING ANIMATIONS ON THE SMM
	 */

	/**
	 * Opens the SMM.
	 */
	public void openSMM() {
		if (shouldLogEvent())
			Log.v(TAG, "internal : openSMM() called.");
		if (isSMMAnimating() || isSMMOpened()) {
			if (shouldLogEvent())
				Log.v(TAG,
						"internal : openSMM() not executed because the menu is animating or already opened.");
			return;
		}
		isSMMAnimating = true;
		if (mSMMFragment != null) {
			mSMMFragment.onSMMAnimating();
		}
		if (mOnSMMAnimatingListener != null) {
			mOnSMMAnimatingListener.onSMMAnimating();
		}

		onSMMAnimating();

		ObjectAnimator anim1 = ObjectAnimator.ofFloat(mContentView, "scaleX",
				1f, 0.9f).setDuration(900);
		anim1.setStartDelay(50);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(mContentView, "scaleY",
				1f, 0.8f).setDuration(500);
		anim2.setStartDelay(000);
		ObjectAnimator anim3 = ObjectAnimator.ofFloat(mContentView, "y", -75f)
				.setDuration(500);
		anim3.setStartDelay(250);
		ObjectAnimator anim4 = ObjectAnimator.ofFloat(mContentView,
				"rotationX", 0f, 5f).setDuration(500);
		anim4.setStartDelay(0);
		ObjectAnimator anim5 = ObjectAnimator.ofFloat(mContentView,
				"rotationX", 5f, 0f).setDuration(300);
		anim5.setStartDelay(500);
		Collection<Animator> animators = new ArrayList<Animator>();
		animators.add(anim1);
		animators.add(anim2);
		animators.add(anim3);
		animators.add(anim4);
		animators.add(anim5);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.addListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
				if (isSMMClosing()) {
					isSMMOpened = false;
					isSMMOpening = true;
					if (mSMMFragment != null) {
						if (mSMMFragment.getActivity() != null) {
							mSMMFragment.onSMMOpening();
						} else {
							if (shouldLogEvent()) {
								Log.e(TAG,
										"The SMM Fragment wasn't able to perform listener onSMMOpening().");
							}
						}

					}
					if (mOnSMMOpeningListener != null) {
						mOnSMMOpeningListener.onSMMOpening();
					}
					onSMMOpening();
				}
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (!isSMMClosed()) {
					isSMMAnimating = false;
					isSMMOpened = true;
					isSMMOpening = false;
					if (mSMMFragment != null) {
						if (mSMMFragment.getActivity() != null) {
							mSMMFragment.onSMMOpened();
						} else {
							if (shouldLogEvent()) {
								Log.e(TAG,
										"The SMM Fragment wasn't able to perform listener onSMMOpened().");
							}
						}
					}
					if (mOnSMMOpenedListener != null) {
						mOnSMMOpenedListener.onSMMOpened();
					}
					onSMMOpened();
				}
			}

			@Override
			public void onAnimationCancel(Animator animation) {
			}
		});
		animatorSet.playTogether(animators);
		animatorSet.start();

		ViewPropertyAnimator.animate(mLayerView).setDuration(800)
				.setListener(new AnimatorListener() {

					@Override
					public void onAnimationStart(Animator animation) {
						mLayerView.setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationRepeat(Animator animation) {
					}

					@Override
					public void onAnimationEnd(Animator animation) {
					}

					@Override
					public void onAnimationCancel(Animator animation) {
					}
				}).alpha(getSMMLayerOpacity());
		ViewPropertyAnimator.animate(mSMMContainer).setDuration(800)
				.y(mStatusBarHeight);
	}

	public void closeSMM() {
		if (shouldLogEvent())
			Log.v(TAG, "internal : closeSMM() called.");
		if (isSMMAnimating() || isSMMClosed()) {
			if (shouldLogEvent())
				Log.v(TAG,
						"internal : closeSMM() not executed because the SMM is animating or it's already closed.");
			return;
		}
		isSMMAnimating = true;

		if (mSMMFragment != null) {
			mSMMFragment.onSMMAnimating();
		}
		if (mOnSMMAnimatingListener != null) {
			mOnSMMAnimatingListener.onSMMAnimating();
		}
		onSMMAnimating();

		ObjectAnimator anim1 = ObjectAnimator.ofFloat(mContentView,
				"rotationX", 0f, 5f).setDuration(350);
		ObjectAnimator anim2 = ObjectAnimator.ofFloat(mContentView,
				"rotationX", 5f, 0f).setDuration(350);
		anim2.setStartDelay(350);
		ObjectAnimator anim3 = ObjectAnimator.ofFloat(mContentView, "y", 0f)
				.setDuration(500);
		anim3.setStartDelay(250);
		ObjectAnimator anim4 = ObjectAnimator.ofFloat(mContentView, "scaleX",
				0.9f, 1f).setDuration(350);
		anim4.setStartDelay(400);
		ObjectAnimator anim5 = ObjectAnimator.ofFloat(mContentView, "scaleY",
				0.8f, 1f).setDuration(350);
		anim5.setStartDelay(400);
		Collection<Animator> animators = new ArrayList<Animator>();
		animators.add(anim1);
		animators.add(anim2);
		animators.add(anim3);
		animators.add(anim4);
		animators.add(anim5);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(animators);
		animatorSet.start();

		ViewPropertyAnimator.animate(mLayerView).setDuration(500)
				.setStartDelay(500).setListener(new AnimatorListener() {

					@Override
					public void onAnimationStart(Animator animation) {
					}

					@Override
					public void onAnimationRepeat(Animator animation) {
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						mLayerView.setVisibility(View.GONE);
						if (!isSMMOpened()) {
							isSMMAnimating = false;
							isSMMOpened = false;
							isSMMOpening = false;
							if (mSMMFragment != null) {
								if (mSMMFragment.getActivity() != null) {
									mSMMFragment.onSMMClosed();
								} else {
									if (shouldLogEvent()) {
										Log.e(TAG,
												"The SMM Fragment wasn't able to perform listener onSMMClosed().");
									}
								}
							}
							if (mOnSMMClosedListener != null) {
								mOnSMMClosedListener.onSMMClosed();
							}
							onSMMClosed();
							mSMMContainer.clearAnimation();
						}
					}

					@Override
					public void onAnimationCancel(Animator animation) {
					}
				}).alpha(0);
		ViewPropertyAnimator.animate(mSMMContainer).setDuration(500)
				.setListener(new AnimatorListener() {

					@Override
					public void onAnimationStart(Animator animation) {
						if (!isSMMOpening()) {
							isSMMOpened = false;
							isSMMOpening = false;
							if (mSMMFragment != null) {
								if (mSMMFragment.getActivity() != null) {
									mSMMFragment.onSMMClosing();
								} else {
									if (shouldLogEvent()) {
										Log.e(TAG,
												"The SMM Fragment wasn't able to perform listener onSMMClosing().");
									}
								}
							}
							if (mOnSMMClosingListener != null) {
								mOnSMMClosingListener.onSMMClosing();
							}
							onSMMClosing();
						}
					}

					@Override
					public void onAnimationRepeat(Animator animation) {
					}

					@Override
					public void onAnimationEnd(Animator animation) {
					}

					@Override
					public void onAnimationCancel(Animator animation) {
					}
				}).setStartDelay(0).y(mDecorViewHeight);
	}

	/**
	 * Toogles the SMM. If it's closed, this method open it. If it's opened,
	 * this method close it.
	 * 
	 * @param forceAnimating
	 *            If true, the SMM will toggle, even if it's already animating.
	 */
	public void toggleSMM(boolean forceAnimating) {
		if (shouldLogEvent())
			Log.v(TAG, "internal : toggleSMM() called.");
		if (isSMMAnimating() && !forceAnimating) {
			return;
		}
		if (isSMMOpening() || isSMMOpened()) {
			closeSMM();
		} else {
			openSMM();
		}
	}

	/**
	 * Notify the activity that the SMM is no longer valid. The activity will
	 * re-execute the method {@link #getSMMFragment()} to change its SMM. <br />
	 * /!\ Caution : This method will change the menu even if it's opened...
	 */
	public void invalidateSMM() {
		if (shouldLogEvent())
			Log.v(TAG, "internal : invalidateSMM() called.");
		if (isSMMAlreadyAttached) {
			mSMMFragment = getSMMFragment();
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.smm_container, mSMMFragment, FRAGMENT_TAG)
					.commit();
			mDecorView.setBackgroundResource(getSMMWindowBackgroundResources());
			if (isSMMOpened()) {
				ViewPropertyAnimator.animate(mLayerView).setDuration(100)
						.alpha(getSMMLayerOpacity());
			}
		} else {
			attachMenuViewToActivity();
		}
	}

	/*
	 * METHODS CHECKING THE SMM'S STATE
	 */

	public SMMFragmentMenu<?> getCurrentSMMFragment() {
		return (SMMFragmentMenu<?>) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
	}

	/**
	 * Check if the SMM is opening. Caution, {@link #isSMMOpening()} will return
	 * FALSE if the SMM is opened.
	 * 
	 * @return TRUE if the SMM is opening. FALSE if the SMM is opened, closing
	 *         or closed.
	 */
	public boolean isSMMOpening() {
		return isSMMAnimating && isSMMOpening;
	}

	/**
	 * Check if the SMM is opened. Caution, {@link #isSMMOpened()} will return
	 * FALSE if the SMM is opening.
	 * 
	 * @return TRUE if the SMM is opened. FALSE if the SMM is opening, closing
	 *         or closed.
	 */
	public boolean isSMMOpened() {
		return !isSMMAnimating && isSMMOpened;
	}

	/**
	 * Check if the SMM is closing. Caution, {@link #isSMMClosing()} will return
	 * FALSE if the SMM is closed.
	 * 
	 * @return TRUE if the SMM is closing. FALSE if the SMM is opening, opened
	 *         or closed.
	 */
	public boolean isSMMClosing() {
		return isSMMAnimating && !isSMMOpening;
	}

	/**
	 * Check if the SMM is closed. Caution, {@link #isSMMClosed()} will return
	 * FALSE if the SMM is closing.
	 * 
	 * @return TRUE if the SMM is closed. FALSE if the SMM is opening, opened or
	 *         closing.
	 */
	public boolean isSMMClosed() {
		return !isSMMAnimating && !isSMMOpened;
	}

	/**
	 * Check if the SMM is animating.
	 * 
	 * @return TRUE if the SMM is closing or opening. FALSE if the SMM is opened
	 *         or closed.
	 */
	public boolean isSMMAnimating() {
		return isSMMAnimating;
	}

	/*
	 * METHODS SETTING LISTENERS
	 */

	/**
	 * Set a listener on the activity.
	 * 
	 * @param listener
	 *            Listener to be called when the SMM is opening.
	 */
	public void setOnSMMOpeningListener(OnSMMOpening listener) {
		mOnSMMOpeningListener = listener;
	}

	/**
	 * Set a listener on the activity.
	 * 
	 * @param listener
	 *            Listener to be called when the SMM is opened.
	 */
	public void setOnSMMOpenedListener(OnSMMOpened listener) {
		mOnSMMOpenedListener = listener;
	}

	/**
	 * Set a listener on the activity.
	 * 
	 * @param listener
	 *            Listener to be called when the SMM is closing.
	 */
	public void setOnSMMClosingListener(OnSMMClosing listener) {
		mOnSMMClosingListener = listener;
	}

	/**
	 * Set a listener on the activity.
	 * 
	 * @param listener
	 *            Listener to be called when the SMM is closed.
	 */
	public void setOnSMMClosedListener(OnSMMClosed listener) {
		mOnSMMClosedListener = listener;
	}

	/**
	 * Set a listener on the activity.
	 * 
	 * @param listener
	 *            Listener to be called when the SMM is animating.
	 */
	public void setOnSMMAnimatingListener(OnSMMAnimating listener) {
		mOnSMMAnimatingListener = listener;
	}

	/*
	 * METHODS CALLED WHEN THE SMM'S STATE IS CHANGING
	 */

	/**
	 * Called when the SMM is opening.
	 */
	public void onSMMOpening() {
	}

	/**
	 * Called when the SMM is opened.
	 */
	public void onSMMOpened() {
	}

	/**
	 * Called when the SMM is closing.
	 */
	public void onSMMClosing() {
	}

	/**
	 * Called when the SMM is closed.
	 */
	public void onSMMClosed() {
	}

	/**
	 * Called when the SMM is animating.
	 */
	public void onSMMAnimating() {
	}

	/*
	 * METHOD CALLED TO OBTAIN THE SMM. YEAH !
	 */

	/**
	 * Method called to create the SMM.
	 * 
	 * @return The Fragment which will be used to create the SMM.
	 */
	public abstract SMMFragmentMenu<?> getSMMFragment();

	/*
	 * METHODS CALLED TO CUSTOMIZE THE SMM
	 */

	/**
	 * Default : 0.6f.
	 * 
	 * @return The opacity (exprimed between 0f and 1f) of the layer appearing
	 *         while the menu is opening. 1f means the content of the activity
	 *         isn't visible while the menu is opened. 0f means the content of
	 *         the activity has the same brightness when the menu is opened.
	 * 
	 */
	public float getSMMLayerOpacity() {
		return 0.6f;
	}

	/**
	 * Default : R.drawable.bg_window_dark.
	 * 
	 * @return the resource used to be the background of the window (the
	 *         background can be seen when the SMM is opening).
	 */
	public int getSMMWindowBackgroundResources() {
		return R.drawable.bg_window_dark;
	}

	/**
	 * Default : FALSE.
	 * 
	 * @return TRUE if you want to see events in LogCat.
	 */
	public boolean shouldLogEvent() {
		return false;
	}

	/**
	 * Default :
	 * 
	 * @return TRUE if you don't want to let the user perform clicks or touchs
	 *         on the activity.
	 */
	public boolean shouldPreventActivityFromTouch() {
		return true;
	}

	/**
	 * Default : TRUE.
	 * 
	 * @return TRUE if you want to close the SMM when the BACK button is pressed
	 *         (and the SMM is opened). This method is not used when the SMM is
	 *         closed.
	 */
	public boolean shouldCloseTheSMMOnBackPressed() {
		return true;
	}

	/**
	 * Default : TRUE
	 * 
	 * @return TRUE if you want to close the menu when the user click outside
	 *         from menu. Be carreful, if the method
	 *         {@link #shouldPreventActivityFromTouch()} returns false, this
	 *         method will not be used...
	 */
	public boolean shouldOnClickOnContentCloseSMMenu() {
		return true;
	}
}