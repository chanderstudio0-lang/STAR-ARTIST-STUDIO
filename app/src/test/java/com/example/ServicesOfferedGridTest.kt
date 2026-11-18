package com.example

import com.example.data.model.ServicePackageCategory
import com.example.data.model.StudioServicesData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicesOfferedGridTest {

    @Test
    fun studioServices_containsMandatoryCategories() {
        val packages = StudioServicesData.packages
        assertTrue("Packages list must not be empty", packages.isNotEmpty())

        val categories = packages.map { it.category }.toSet()
        assertTrue("Must include Photography", categories.contains(ServicePackageCategory.PHOTOGRAPHY))
        assertTrue("Must include Modeling", categories.contains(ServicePackageCategory.MODELING))
        assertTrue("Must include Production", categories.contains(ServicePackageCategory.PRODUCTION))
    }

    @Test
    fun studioServices_pricingAndInclusions_validAndTransparent() {
        val packages = StudioServicesData.packages

        packages.forEach { pkg ->
            assertTrue("Price must be greater than zero for ${pkg.id}", pkg.startingPriceInr > 0)
            assertTrue("English formatted price must contain rupee symbol: ${pkg.priceFormattedEn}", pkg.priceFormattedEn.contains("₹"))
            assertTrue("Hindi formatted price must contain rupee symbol: ${pkg.priceFormattedHi}", pkg.priceFormattedHi.contains("₹"))
            assertTrue("Title English must not be blank", pkg.titleEn.isNotBlank())
            assertTrue("Title Hindi must not be blank", pkg.titleHi.isNotBlank())
            assertTrue("Summary English must not be blank", pkg.summaryEn.isNotBlank())
            assertTrue("Summary Hindi must not be blank", pkg.summaryHi.isNotBlank())
            assertTrue("Package highlights must include at least 3 deliverables for ${pkg.id}", pkg.highlightsEn.size >= 3)
            assertEquals("English and Hindi highlights count must match for ${pkg.id}", pkg.highlightsEn.size, pkg.highlightsHi.size)
            assertNotNull("Package must provide an icon", pkg.icon)
            assertTrue("Recommended service type must not be blank for booking linkage", pkg.recommendedServiceType.isNotBlank())
        }
    }

    @Test
    fun studioServices_categoryFiltering_worksAccurately() {
        val allPackages = StudioServicesData.packages

        val photoPackages = allPackages.filter { it.category == ServicePackageCategory.PHOTOGRAPHY }
        val modelPackages = allPackages.filter { it.category == ServicePackageCategory.MODELING }
        val productionPackages = allPackages.filter { it.category == ServicePackageCategory.PRODUCTION }

        assertTrue("Photography packages must contain items", photoPackages.isNotEmpty())
        assertTrue("Modeling packages must contain items", modelPackages.isNotEmpty())
        assertTrue("Production packages must contain items", productionPackages.isNotEmpty())

        assertEquals(allPackages.size, photoPackages.size + modelPackages.size + productionPackages.size)
    }

    @Test
    fun studioServices_popularFlagPresent() {
        val popularPackages = StudioServicesData.packages.filter { it.isPopular }
        assertTrue("There must be signature popular packages highlighted", popularPackages.isNotEmpty())
    }

    @Test
    fun studioServices_categoryLabelsAndFavoritesFiltering_matchRequirements() {
        assertEquals("Photography", ServicePackageCategory.PHOTOGRAPHY.titleEn)
        assertEquals("Modeling", ServicePackageCategory.MODELING.titleEn)
        assertEquals("Video Production", ServicePackageCategory.PRODUCTION.titleEn)

        val savedIds = setOf("pkg_photo_fashion", "pkg_video_music")
        val favoritesAll = StudioServicesData.filterAndSortPackages(
            favoritesOnly = true,
            favoriteIds = savedIds
        )
        assertEquals(listOf("pkg_photo_fashion", "pkg_video_music"), favoritesAll.map { it.id })

        val favoritesVideoOnly = StudioServicesData.filterAndSortPackages(
            category = ServicePackageCategory.PRODUCTION,
            favoritesOnly = true,
            favoriteIds = savedIds
        )
        assertEquals(listOf("pkg_video_music"), favoritesVideoOnly.map { it.id })
    }
}
