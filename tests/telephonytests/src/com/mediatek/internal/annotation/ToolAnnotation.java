package com.mediatek.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//use below command to run test case marked with annotation of "GoogleAnnotation"
//adb shell am instrument -w -e annotation com.android.gallery3d.autotest.annotation.GoogleAnnotation

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ToolAnnotation {
    
}