package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.actions.PhoneActionManager
import com.example.ai.AssistantIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Assistant Voice", appName)
  }

  @Test
  fun `phone action manager returns valid time and date`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = PhoneActionManager(context)
    val timeResult = manager.getCurrentTime()
    val dateResult = manager.getCurrentDate()
    assertTrue(timeResult.success)
    assertTrue(dateResult.success)
    assertTrue(timeResult.messageSi.isNotBlank())
    assertTrue(dateResult.messageSi.isNotBlank())
    manager.cleanup()
  }

  @Test
  fun `assistant intent parses safely`() {
    assertEquals(AssistantIntent.OPEN_APP, AssistantIntent.fromWire("OPEN_APP"))
    assertEquals(AssistantIntent.CHAT, AssistantIntent.fromWire("UNKNOWN_VALUE"))
  }
}
