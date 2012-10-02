package org.citisense.android;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class AQIDetailsView extends ViewGroup {
	AQIGraphView graphView;
	View detailedView;
	
	final static double WIDTH_RATIO = 1.0;
	final static double HEIGHT_RATIO = 0.4;
	
	AQIDetailsView (Context context, AQIGraphView gView, View dView) {
		super(context);
		graphView = gView;
		detailedView = dView;
		
		graphView.setLayoutParams(new ViewGroup.LayoutParams((int)(getWidth() * WIDTH_RATIO), (int)(getHeight() * HEIGHT_RATIO)));
		
		//addView(detailedView);
		addView(graphView);
	}
	
	@Override
	protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}

}
