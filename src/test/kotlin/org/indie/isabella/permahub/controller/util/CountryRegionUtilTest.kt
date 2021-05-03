package org.indie.isabella.permahub.controller.util

import org.indie.isabella.permahub.exception.BadInputException
import org.indie.isabella.permahub.model.Area
import org.indie.isabella.permahub.utils.CountryRegionUtil
import org.json.JSONArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CountryRegionUtilTest {
    @Test
    fun `validate should throw exception if region is empty but country is valid`() {
        assertThrows<BadInputException>("'XY' is invalid country, please refer to ISO3166-2") {
            CountryRegionUtil.validate(Area("XY"))
        }
    }
    @Test
    fun `validate should not throw exception if region is empty but country is valid`() {
        assertDoesNotThrow("Should not throw BadInputException") {
            CountryRegionUtil.validate(Area("ID"))
        }
    }

    @Test
    fun `validate should throw exception if region is invalid`() {
        assertThrows<BadInputException>("'ssss' is invalid region of 'ID', please refer to ISO3166-2") {
            CountryRegionUtil.validate(Area("ID", "ssss"))
        }
    }

    @Test
    fun `validate should not throw exception if region and country is valid`() {
        assertDoesNotThrow("Should not throw BadInputException") {
            CountryRegionUtil.validate(Area("ID", "JI"))
        }
    }


    @Test
    fun testtttt() {
        val x = JSONArray(this::class.java.getResource("/country-region.json").readText())
        for(i in 0 until x.length()) {
            val country = x.getJSONObject(i).get("countryShortCode")
            val regions = x.getJSONObject(i).getJSONArray("regions")
            val regionCodes = arrayListOf<String>()
            for(j in 0 until regions.length()) {
                if (!regions.getJSONObject(j).isNull("shortCode"))
                    regionCodes.add("\""+ regions.getJSONObject(j).getString("shortCode")+"\"")

                else if (!regions.getJSONObject(j).isNull("code"))
                    regionCodes.add("\""+ regions.getJSONObject(j).getString("code") + "\"")
                else
                    regionCodes.add("\""+ regions.getJSONObject(j).getString("name") + "\"")
            }
            println("\"$country\" to listOf(" + regionCodes.joinToString(", ") + "),")
        }
    }
}