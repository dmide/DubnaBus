package ru.ratadubna.dubnabus;

import android.os.Bundle;

public class SimpleContentFragment extends AbstractContentFragment {
	private static final String KEY_DATA = "data";

	protected static SimpleContentFragment newInstance(String file) {
		SimpleContentFragment f = new SimpleContentFragment();
		Bundle args = new Bundle();
		args.putString(KEY_DATA, file);
		f.setArguments(args);
		return (f);
	}

	@Override
	String getPage() {
		return (getArguments().getString(KEY_DATA));
	}
}