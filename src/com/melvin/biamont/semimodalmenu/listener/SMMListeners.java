package com.melvin.biamont.semimodalmenu.listener;

public class SMMListeners {

	public static interface OnSMMOpening {
		public void onSMMOpening();
	}

	public static interface OnSMMOpened {
		public void onSMMOpened();
	}

	public static interface OnSMMClosing {
		public void onSMMClosing();
	}

	public static interface OnSMMClosed {
		public void onSMMClosed();
	}

	public static interface OnSMMAnimating {
		public void onSMMAnimating();
	}
}
