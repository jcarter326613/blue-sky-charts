package com.blueskycharts.app.map.models

data class WeatherConditionResponse (
    var oldestDataAgeSeconds: Int? = null,
    var conditions: Array<WeatherCondition>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherConditionResponse

        if (oldestDataAgeSeconds != other.oldestDataAgeSeconds) return false
        val conditions = this.conditions
        if (conditions != null) {
            val otherConditions = other.conditions ?: return false
            if (!conditions.contentEquals(otherConditions)) return false
        } else if (other.conditions != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = oldestDataAgeSeconds ?: 0
        result = 31 * result + (conditions?.contentHashCode() ?: 0)
        return result
    }
}