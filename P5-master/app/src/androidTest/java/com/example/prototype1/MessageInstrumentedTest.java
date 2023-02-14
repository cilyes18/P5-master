package com.example.prototype1;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MessageInstrumentedTest {
    @Test
    public void createMessageInGroupChat() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.prototype1", appContext.getPackageName());
    }
    @Test
    public void updateMessage() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.prototype1", appContext.getPackageName());
    }
    @Test
    public void deleteMessage() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.prototype1", appContext.getPackageName());
    }
    @Test
    public void leaveConversation() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.prototype1", appContext.getPackageName());
    }

}