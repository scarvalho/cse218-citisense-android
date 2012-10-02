package org.citisense.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.View;

/**
 * AQIGraphView creates a line graph of given AQI data points. Points and lines
 * are colored based on AQI levels they fall under. The graph's labels adjust
 * to allow variable numbers of input data points.
 * 
 * @author Erica Klein, Arno den Hond
 */
public class AQIGraphView extends View {
	/*
	 * graph aesthetics constants
	 */
	private static final double DRAW_CIRCLE_RADIUS = 3.0;
	private static final double DRAW_BIG_CIRCLE_RADIUS = 5.0;
	private static final float LINE_WIDTH = 3.0f;
	private static final float MIN_MAX_LINE_WIDTH = 1.0f;
	// TOTAL_HOURS must be divisible by NUM_TIME_INTERVALS
	private static final int TOTAL_HOURS = 24;
	private static final int NUM_TIME_INTERVALS = 6;
	
	/*
	 * graph specifications
	 */
	// use decimal format only, do not do like 1/3, it will be 0.0
	private static final double RATIO_OF_SCREEN_HEIGHT = 1.0;
	private static final int BORDER = 20;
	// spacing between right edge of graph and right edge of screen
	// use decimal format only, do not do like 2/3, it will be 0.0
	private static final double RATIO_OF_BORDER = 0.85;
	private static final int MAX_LABEL = 100;
	private static final int LABEL_INCREMENT = 25;
	// pixel offset from left side of graph (which right now is edge of screen)
	private static final int LABEL_OFFSET = BORDER / 3;
	private static final int TICK_HEIGHT = 4;
	
	/*
	 * graph components and data
	 */
	private Canvas canvas;
	private double[] values;
	private String title = "miAQI History";
	private double minPointX = -1;
	private double minPointY = -1;
	private double maxPointX = -1;
	private double maxPointY = -1;
	private double startingXCoord = BORDER * 2;
	private double totalGraphHeight;
	private double totalGraphWidth;
	private double innerGraphHeight;
	private double innerGraphWidth;
	private Calendar calendar;
	
	/*
	 * objects for display (color, labels)
	 */
	private Paint textPaint;
	private Paint backgroundGraphPaint;
	private ArrayList<String> timeLabels = new ArrayList<String>();
	private ArrayList<Integer> aqiLabels;
	private AQILevel[] aqiLevels = new AQILevel[AQILevel.NUM_LEVELS];
	private Paint[] aqiLevelPaint = new Paint[aqiLevels.length];
	
	/**
	 * Initiates the graph variables but does not draw anything yet.
	 * 
	 * @param context
	 * @param values: array of double AQI values to be graphed
	 */
	public AQIGraphView(Context context, double[] values) {
		super(context);
		init(values);
	}
	
	public AQIGraphView(Context context, AttributeSet attr) {
		super(context, attr);
		init(new double[]{-1});
//		setLayoutParams(new ViewGroup.LayoutParams(context, attr));
	}
	
	public AQIGraphView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
		init(new double[]{-1});
	}
	
	private void init(double[] values) {
		if (values == null) {
			values = new double[TOTAL_HOURS];
			for (int i = 0; i < values.length; i++) {
				values[i] = -1;
			}
		}
		
		this.values = sanitizeAQIData(values);
		
		initAQILevels();
		initTimeLabels();
		initPaint();
	}
	
	public void setValues(double[] values) {
		this.values = sanitizeAQIData(values);
	}
	
	/**
	 * Takes any values over AQILevel.MAX_AQI and reduces them to the max. Also
	 * if size of array is less than TOTAL_HOURS, it makes it size TOTAL_HOURS
	 * and fills the end of it with -1.
	 * @param values: array of double AQI values
	 * @return the parameter array, with any oversize values reduced to MAX_AQI
	 */
	private double[] sanitizeAQIData(double[] values) {
		calendar = new GregorianCalendar();
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		double[] sanitized = new double[TOTAL_HOURS];
		int i;
		for (i = 0; i < values.length && i <= hour; i++) {	
			sanitized[i] =
				values[i] > AQILevel.MAX_AQI ? AQILevel.MAX_AQI : values[i];
			// allow 0 MAX AQIs to be shown for debugging purposes (actually this is acceptable as 0 != no value...)
			//if( sanitized[i] == 0) sanitized[i] = -1; 
		}
		
		// fill rest of array
		for (; i < sanitized.length; i++) {
			sanitized[i] = -1;
		}
		return sanitized;
	}

	/**
	 * Initiates the levels array to hold an AQILevel object for each level.
	 */
	private void initAQILevels() {
		aqiLevels[AQILevel.GOOD] = new AQILevel(AQILevel.GOOD);
		aqiLevels[AQILevel.MODERATE] = new AQILevel(AQILevel.MODERATE);
		aqiLevels[AQILevel.UNHEALTHY_SENSITIVE] = new AQILevel(AQILevel.UNHEALTHY_SENSITIVE);
		aqiLevels[AQILevel.UNHEALTHY] = new AQILevel(AQILevel.UNHEALTHY);
		aqiLevels[AQILevel.VERY_UNHEALTHY] = new AQILevel(AQILevel.VERY_UNHEALTHY);
		aqiLevels[AQILevel.HAZARDOUS] = new AQILevel(AQILevel.HAZARDOUS);
	}
	
	/**
	 * Initiates labels array with the labels on the vertical axis. Difference
	 * between labels is designated by a constant increment specified above.
	 */
	private void initAQILabels() {
		aqiLabels = new ArrayList<Integer>();
		int increment = LABEL_INCREMENT;
		
		if (getMax() >= 350) {
			increment = 50;
		} else if (getMax() >= 250) {
			increment = 40;
		} else if (getMax() >= 150) {
			increment = 30;
		}
		
		int label = 0;
		// initialize with labels 0 to default
		for (; label <= MAX_LABEL; label += increment) {
			aqiLabels.add(0, label);
		}

		label -= increment;
		// add more labels if necessary
		while (getMax() > label) {
			label += increment;
			aqiLabels.add(0, label);
		}
	}
	
	/**
	 * Initiates time labels array with default labels.
	 */
	private void initTimeLabels() {
		timeLabels = new ArrayList<String>();
		int interval = TOTAL_HOURS / NUM_TIME_INTERVALS;
		
		for (int i = 0, currentHour = 0; i < NUM_TIME_INTERVALS + 1; i++, currentHour += interval) {
			String time = "";
			
			// midnight
			if (currentHour == 0 || currentHour == 24) {
				time = "12am";
			} else if (currentHour > 12) {
				// after noon
				time += currentHour - 12;
				time += "pm";
			} else if (currentHour == 12) {
				// noon
				time = "12pm";
			} else {
				// morning
				time += currentHour;
				time += "am";
			}
			
			timeLabels.add(time);
		}
	}

	/**
	 * Initiates paint objects for levels as well as for aesthetics colors.
	 */
	private void initPaint() {
		Paint tempPaint = new Paint();
		tempPaint.setAntiAlias(true);
		tempPaint.setStrokeWidth(LINE_WIDTH);
		for (int i = 0; i < aqiLevelPaint.length; i++) {
			aqiLevelPaint[i] = new Paint(tempPaint);
			aqiLevelPaint[i].setColor(aqiLevels[i].getColor());
		}
		
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setFakeBoldText(true);
		textPaint.setColor(Color.WHITE);
		
		backgroundGraphPaint = new Paint();
		backgroundGraphPaint.setAntiAlias(true);
		backgroundGraphPaint.setColor(Color.DKGRAY);
	}

	/*
	 * still to do:
	 * colored vertical labels -- try to have lighter colors so they don't take away from the graph so much?
	 * draw min, max, avg(?) lines...should they have data values? and where to put those values?
	 * 		but the values could potentially overwrite the lines...really hard to tell!
	 * put in details activity
	 * Try to see what it looks like as a bar graph instead
	 */
	/**
	 * Draws the graph.
	 */
	@Override
	protected void onDraw(Canvas c) {
		canvas = c;

		totalGraphHeight = getHeight() * RATIO_OF_SCREEN_HEIGHT;
		totalGraphWidth = getWidth() - BORDER * RATIO_OF_BORDER;
		innerGraphHeight = totalGraphHeight - (2 * BORDER);
		innerGraphWidth = totalGraphWidth - (2 * BORDER);
		
		initAQILabels();
		drawLabelsAndBackground();

		// draw title
		textPaint.setTextAlign(Align.CENTER);
		drawText(title, (innerGraphWidth / 2) + startingXCoord, BORDER - 4,
				 textPaint);

		drawData();
		
		// draw min and max lines
		// TODO: decide if we really need to highlight max / min points.  For now skipping drawing anything extra
		//drawMinMaxLinesAndPoints();
	}

	/**
	 * Draws vertical and horizontal labels on the graph and the background
	 * lines.
	 */
	private void drawLabelsAndBackground() {
		textPaint.setTextAlign(Align.LEFT);
		int numRows = aqiLabels.size() - 1;
		// draw horizontal markers on graph and vertical labels
		for (int i = 0; i < aqiLabels.size(); i++) {
			double y = ((innerGraphHeight / numRows) * i) + BORDER;
			drawLine(startingXCoord, y, totalGraphWidth, y, backgroundGraphPaint);
			textPaint.setColor(
					aqiLevelPaint[AQILevel.getLevel(aqiLabels.get(i))].getColor());
					//aqiLevels[AQILevel.getLevel(aqiLabels.get(i))].getFadeColor());
			drawText("" + aqiLabels.get(i), LABEL_OFFSET, y, textPaint);
		}
		
		textPaint.setColor(Color.WHITE);
		textPaint.setTextAlign(Align.CENTER);
		int numColumns = timeLabels.size() - 1;
		
		// draw tick marks after line
		int numInternalCols = TOTAL_HOURS / NUM_TIME_INTERVALS;
		double width = innerGraphWidth / (numColumns * numInternalCols);
		
		//draw vertical markers on graph and horizontal labels
		for (int i = 0; i < timeLabels.size(); i++) {
			double x = ((innerGraphWidth / numColumns) * i) + startingXCoord;
			drawLine(x, totalGraphHeight - BORDER, x, BORDER, backgroundGraphPaint);
			drawText(timeLabels.get(i), x, totalGraphHeight - 4, textPaint);

			// ticks per larger column
			int ticks = numInternalCols - 1;
			
			// don't draw ticks for last one
			if (i < numColumns) {
				for (; ticks > 0; ticks--) {
					double tickX = x + ticks * width;
					drawLine(tickX, innerGraphHeight + BORDER, tickX, innerGraphHeight + BORDER - TICK_HEIGHT, backgroundGraphPaint);
				}
			}
		}
		double x = innerGraphWidth + startingXCoord;
		drawLine(x, totalGraphHeight - BORDER, x, BORDER, backgroundGraphPaint);
	}
	
	/**
	 * Draws data (lines and points) on the graph and keeps track of the min
	 * and max points to make them bigger later on.
	 */
	private void drawData() {
		double columnWidth = innerGraphWidth / TOTAL_HOURS;
		double lastHeight = 0;
		for (int i = 0; i < values.length; i++) {
			double currentYValue = values[i];
			
			// to skip invalid values
			if (currentYValue < 0) {
				lastHeight = (double)-1;
				continue;
			}
			
			double currentHeight =
						innerGraphHeight * (currentYValue / getMaxAQILabel());
			
			double lastColumnWidth;
			// this only happens if it's the last data point (current reading)
			if (i == calendar.get(Calendar.HOUR_OF_DAY)) {
				lastColumnWidth = columnWidth * (calendar.get(Calendar.MINUTE) / 60.0);
			} else {
				lastColumnWidth = columnWidth;
			}
			
			double drawX = i * columnWidth + lastColumnWidth + startingXCoord;
			double drawY = (BORDER - currentHeight) + innerGraphHeight;
			
			// check if it's a new min or max value
			if (i == getMaxIndex()) {
				maxPointX = drawX;
				maxPointY = drawY;
			}
			
			if (i == getMinIndex()) {
				minPointX = drawX;
				minPointY = drawY;
			}
			
			// to not connect the -1 values with the current value
			// this is put after the calculations so we get the min/max saved
			if (lastHeight < 0) {
				// if the last value was invalid, don't connect with a line
				lastHeight = currentHeight;
				continue;
			}
			
			// draw the actual line
			if (i > 0)
				drawAQILine(drawX - lastColumnWidth,
							(BORDER - lastHeight) + innerGraphHeight,
						    drawX, drawY,
						    values[i], values[i-1]);
			
			// save most recent height value
			lastHeight = currentHeight;
		}
	}

	/**
	 * Draw the horizontal lines through the min and max points and draw the
	 * bigger points over the min and max values. Does not draw line or point
	 * if the min or max is not valid.
	 */
	private void drawMinMaxLinesAndPoints() {		
		Paint minPaint = new Paint(aqiLevelPaint[AQILevel.getLevel(getMin())]);
		minPaint.setStrokeWidth(MIN_MAX_LINE_WIDTH);
		
		Paint maxPaint = new Paint(minPaint);
		maxPaint.setColor(aqiLevelPaint[AQILevel.getLevel(getMax())].getColor());

		if (minPointY >= 0) {
			drawLine(startingXCoord, minPointY, startingXCoord + innerGraphWidth,
					 minPointY, minPaint);
			// draw point
			drawCircle(minPointX, minPointY, DRAW_BIG_CIRCLE_RADIUS,
					aqiLevelPaint[AQILevel.getLevel(getMin())]);
		}
		
		if (maxPointY >= 0) {
			drawLine(startingXCoord, maxPointY, startingXCoord + innerGraphWidth,
					 maxPointY, maxPaint);
			// draw point
			drawCircle(maxPointX, maxPointY, DRAW_BIG_CIRCLE_RADIUS,
					aqiLevelPaint[AQILevel.getLevel(getMax())]);
		}
	}
	
	/**
	 * Draw a line from (startX, startY) to (endX, endY), including endpoints.
	 * 
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param curAQI: the current AQI value
	 * @param prevAQI: the last AQI value
	 */
	private void drawAQILine(double startX, double startY, double endX,
							 double endY, double curAQI, double prevAQI) {
		if (AQILevel.getLevel(prevAQI) == AQILevel.getLevel(curAQI)) {
			drawSingleLevelData(startX, startY, endX, endY, prevAQI);
		} else {
			drawMultipleLevelsData(startX, startY, endX, endY, curAQI, prevAQI);
		}
		
	}

	/**
	 * Draws a multicolored line from (startX, startY) to (endX, endY) and
	 * endpoints in the event that the line crosses multiple levels.
	 * 
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param curAQI: the current AQI value
	 * @param prevAQI: the last AQI value
	 */
	private void drawMultipleLevelsData(double startX, double startY,
										double endX, double endY,
										double curAQI,double prevAQI) {
		// line must change color somewhere, or multiple places
		// what if the line changes at an endpoint? can i draw a line that is just one point? or will it complain?
		int numLevels = AQILevel.getLevel(curAQI) - AQILevel.getLevel(prevAQI);
		boolean incrementing = numLevels > 0;
		numLevels = numLevels < 0 ? -1 * numLevels : numLevels;
		
		AQILevel[] levels = new AQILevel[numLevels + 1];
		
		// get levels set up in array
		for (int i = 0, level = AQILevel.getLevel(prevAQI); i < numLevels + 1;
																	i++) {
			levels[i] = aqiLevels[level];
			
			if (incrementing) {
				level++;
			} else {
				level--;
			}
		}
		
		// draw the first point
		drawCircle(startX, startY, DRAW_CIRCLE_RADIUS,
				   aqiLevelPaint[AQILevel.getLevel(prevAQI)]);
		
		// draw actual multi-color line
		drawDataLine(startX, startY, endX, endY, curAQI, prevAQI, incrementing,
					 levels);
		// draw the second point
		drawCircle(endX, endY, DRAW_CIRCLE_RADIUS,
				   aqiLevelPaint[AQILevel.getLevel(curAQI)]);
	}

	/**
	 * Draws a line (without endpoints) from (startX, startY) to (endX, endY)
	 * that crosses multiple levels.
	 * 
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param curAQI: the current AQI value
	 * @param prevAQI: the last AQI value
	 * @param incrementing: true if the line's slope is positive, false otherwise
	 * @param levels: array of AQILevel objects representing the levels the line
	 * 				  crosses, in order
	 */
	private void drawDataLine(double startX, double startY, double endX,
			double endY, double curAQI, double prevAQI, boolean incrementing,
			AQILevel[] levels) {
		// if line's slope is negative, reverse start and end and levels array
		// this is so colored lines match up
		if (!incrementing) {
			double temp = endY;
			endY = startY;
			startY = temp;
			
			temp = endX;
			endX = startX;
			startX = temp;
			
			temp = prevAQI;
			prevAQI = curAQI;
			curAQI = temp;
			
			incrementing = incrementing ? false : true;
			
			List<AQILevel> list = Arrays.asList(levels);
			Collections.reverse(list);
			levels = (AQILevel[])list.toArray();
		}
		
		double previousX = startX;
		double previousY = startY;
		double previousAQI = prevAQI;
		
		//TODO: does this hold for endpoints? where diff is 0?
		for (int i = 0; i < levels.length; i++) {
			double diff, ratio;
			
			if (incrementing) {
				if (i == levels.length - 1) {
					// last one, set to last AQI
					diff = curAQI - previousAQI;
				} else {
					diff = (double)levels[i].getRangeMax() - previousAQI;
				}
				ratio = diff / (curAQI - prevAQI);
			} else {
				if (i == levels.length - 1) {
					// last one, set to last AQI
					diff = previousAQI - curAQI;
				} else {
					diff = previousAQI - (double)levels[i].getRangeMin();
				}
				ratio = diff / (prevAQI - curAQI);
			}
			
			double yChange = (endY - startY) * ratio;
			double xChange = (endX - startX) * ratio;
			
			drawLine(previousX, previousY, previousX + xChange,
					 previousY + yChange, aqiLevelPaint[levels[i].getKey()]);
			
			// update
			previousX = previousX + xChange;
			previousY = previousY + yChange;
			
			if (incrementing) {
				if (i == levels.length - 1) {
					// last one, set to last AQI
					previousAQI = curAQI;
				} else {
					previousAQI = levels[i].getRangeMax() + 1;
				}
			} else {
				if (i == levels.length - 1) {
					previousAQI = curAQI;
				} else {
					previousAQI = levels[i].getRangeMin() - 1;	
				}
			}
		}
	}

	/**
	 * Draw the line from (startX, startY) to (endX, endY) with endpoints.
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 * @param level: the current level
	 */
	private void drawSingleLevelData(double startX, double startY, double endX,
			double endY, double level) {
		drawLine(startX, startY, endX, endY,
				 aqiLevelPaint[AQILevel.getLevel(level)]);
		drawCircle(startX, startY, DRAW_CIRCLE_RADIUS,
				   aqiLevelPaint[AQILevel.getLevel(level)]);
		drawCircle(endX, endY, DRAW_CIRCLE_RADIUS,
				   aqiLevelPaint[AQILevel.getLevel(level)]);
	}
	
	/**
	 * Draw a line from (sx, sy) to (ex, ey) with the properties specified in
	 * paint.
	 * 
	 * @param sx
	 * @param sy
	 * @param ex
	 * @param ey
	 * @param paint
	 */
	private void drawLine(double sx, double sy, double ex, double ey,
						  Paint paint) {
		canvas.drawLine((float)sx, (float)sy, (float)ex, (float)ey, paint);
	}
	
	/**
	 * Print text at the point (x, y) with the properties specified in paint.
	 * @param string
	 * @param x
	 * @param y
	 * @param paint
	 */
	private void drawText(String string, double x, double y, Paint paint) {
		canvas.drawText(string, (float)x, (float)y, paint);
	}
	
	/**
	 * Print a circle at the point (x, y) with the radius drawCircleRadius with
	 * the properties specified in paint.
	 * @param x
	 * @param y
	 * @param drawCircleRadius
	 * @param paint
	 */
	private void drawCircle(double x, double y, double drawCircleRadius,
						    Paint paint) {
		canvas.drawCircle((float)x, (float)y, (float)drawCircleRadius, paint);
	}
	
	/**
	 * @return the index of the maximum value in values[].
	 */
	private int getMaxIndex() {
		double largest = Integer.MIN_VALUE;
		int index = 0;
		for (int i = 0; i < values.length; i++)
			if (values[i] >= largest) {
				largest = values[i];
				index = i;
			}
		return index;
	}

	/**
	 * @return the maximum AQI value in values[].
	 */
	private double getMax() {
		return values[getMaxIndex()];
	}
	
	/**
	 * @return the index of the minimum value in values[].
	 */
	private int getMinIndex() {
		double smallest = Integer.MAX_VALUE;
		int index = 0;
		for (int i = 0; i < values.length; i++)
			if (values[i] >= 0 && values[i] <= smallest) {
				smallest = values[i];
				index = i;
			}
		return index;
	}

	/**
	 * @return the minimum AQI value in values[].
	 */
	private double getMin() {
		return values[getMinIndex()];
	}
	
	/**
	 * @return the maximum AQI label used for this graph.
	 */
	private int getMaxAQILabel() {
		return aqiLabels.get(0);
	}

}
