package org.literacybridge.androidtbloader.util.content_manager

import android.location.Location
import java.util.Locale

/**
 * Information about a community in a project.
 */
class CommunityInfo {
    val name: String
    val project: String
    var location: Location?

    constructor(name: String, project: String) {
        this.name = name
        this.project = project
        location = null
    }

    constructor(name: String, project: String, location: Location?) {
        this.name = name
        this.project = project
        this.location = location
    }

    constructor(name: String, project: String, latitude: Double, longitude: Double) {
        val l = Location("cached")
        l.latitude = latitude
        l.longitude = longitude
        this.name = name
        this.project = project
        location = l
    }

    /**
     * Make a string of project and community names that can be passed through "extra" data.
     * @return The string, suitable for extra data.
     */
    fun makeExtra(): String {
        return name + DELIMITER + project
    }

    override fun hashCode(): Int {
        return name.uppercase(Locale.getDefault())
            .hashCode() xor project.uppercase(Locale.getDefault()).hashCode()
    }

    /**
     * Equal if same community and same project.
     */
    override fun equals(o: Any?): Boolean {
        if (o !is CommunityInfo) return false
        var eql = name.equals(o.name, ignoreCase = true)
        if (eql) {
            eql = project.equals(o.project, ignoreCase = true)
        }
        return eql
    }

    override fun toString(): String {
        return name
    }

    companion object {
        private const val DELIMITER = "\u205c" // ⁜, dotted cross

        /**
         * Given a string from extra data, return a CommunityInfo. If location is known for the
         * community, it will be present in the CommunityInfo.
         * @param delimited The string from "makeExtra"
         * @return The corresponding CommunityInfo.
         */
        fun parseExtra(delimited: String): CommunityInfo? {
            val result: CommunityInfo? = null
            val parts = delimited.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // TODO: implement known locations
//        if (parts.length == 2) {
//            result = KnownLocations.findCommunity(parts[1], parts[0]);
//            if (result == null) {
//                result = new CommunityInfo(parts[0], parts[1]);
//            }
//        }
            return result
        }

        fun makeExtra(infos: Collection<CommunityInfo>): ArrayList<String> {
            val result = ArrayList<String>()
            for (info in infos) {
                result.add(info.makeExtra())
            }
            return result
        }

        fun parseExtra(list: List<String>): ArrayList<CommunityInfo?> {
            val result = ArrayList<CommunityInfo?>()
            for (extra in list) {
                result.add(parseExtra(extra))
            }
            return result
        }
    }
}
