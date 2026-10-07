package dev.tabletop.klondike.ui

import dev.tabletop.feedback.Feedback
import dev.tabletop.feedback.FeedbackEvent

class RecordingFeedback : Feedback {
    val events = mutableListOf<FeedbackEvent>()
    override fun play(event: FeedbackEvent) { events += event }
}
