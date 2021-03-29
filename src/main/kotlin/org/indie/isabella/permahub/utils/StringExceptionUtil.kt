package org.indie.isabella.permahub.utils

class StringExceptionUtil {
    companion object {
        fun getValue(message: String, field: String, endLimiter: String, includeLimiter: Boolean = false): String {
            val positionOfField = message.indexOf(field)
            val positionOfValue = positionOfField + field.length + 1 + includeLimiter.not().compareTo(false)
            val endPositionOfValue = message.indexOf(endLimiter, positionOfValue) +includeLimiter.compareTo(false)
            return message.substring(positionOfValue, endPositionOfValue)
        }
    }
}