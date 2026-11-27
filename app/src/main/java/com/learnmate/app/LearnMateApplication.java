package com.learnmate.app;

import android.app.Application;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class LearnMateApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
    }
}
