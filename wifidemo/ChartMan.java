package com.jhard.wifidemo;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Justin on 4/7/2018.
 */

public class ChartMan
{
    LineChart chart;
    List<Entry> entries;
    LineData lineData;
    LineDataSet dataSet;
    public ChartMan(LineChart _chart, Context context)
    {
        chart = _chart;
        entries = new ArrayList<Entry>();
        /*for(int i = 0; i < 2000; i++)
        {
            entries.add(new Entry(i, (int)(Math.random()*50)));
        }*/
        dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(context.getResources().getDrawable(R.drawable.chart_fill, null));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineData = new LineData(dataSet);
        AxisBase axis = chart.getAxisLeft();
        axis.setAxisLineWidth(2);
        axis.setAxisLineColor(Color.BLACK);
        axis.setDrawGridLines(false);
        axis = chart.getAxisRight();
        axis.setAxisLineWidth(2);
        axis.setAxisLineColor(Color.BLACK);
        axis.setDrawGridLines(false);
        axis = chart.getXAxis();
        axis.setAxisLineWidth(2);
        axis.setAxisLineColor(Color.BLACK);
        axis.setDrawGridLines(false);
        axis.setAxisMaximum(entries.size());
        axis.setAxisMinimum(entries.size() - 10);
    }
    public List<Entry> GetEntries()
    {
        return entries;
    }
    void addValue(float val)
    {
        entries.add(new Entry(entries.size(), val));
        if(entries.size() > 8000)
        {
            entries.remove(0);
        }
        dataSet.notifyDataSetChanged();
        AxisBase axis = chart.getAxisLeft();
        axis.setAxisMaximum(dataSet.getYMax());
        axis.setAxisMinimum(dataSet.getYMin());
        axis = chart.getAxisRight();
        axis.setAxisMaximum(dataSet.getYMax());
        axis.setAxisMinimum(dataSet.getYMin());
        axis = chart.getXAxis();
        axis.setAxisMaximum(entries.size());
        axis.setAxisMinimum(entries.size() >= 10 ? entries.size() - 10 : 0);
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        //chart.animateXY(500, 500);
        chart.invalidate();
    }
}
