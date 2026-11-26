package vn.rally.model.bo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogBOTest {
    private static final CatalogBO.ProductFields PRODUCT = new CatalogBO.ProductFields(
            "2", " Vợt Yonex ", " Yonex ", "", "1250000", "", true);
    private static final CatalogBO.VariantFields NEW_VARIANT = new CatalogBO.VariantFields(
            "", "ASTROX-4U", " 4U ", "6", "", false);
    private static final CatalogBO.VariantFields BLANK_VARIANT = new CatalogBO.VariantFields(
            "", "", "", "", "", false);

    @Test
    void validatesVietnameseProductAndIgnoresBlankOptionalVariant() throws Exception {
        var result = CatalogBO.validateProduct(0, PRODUCT, List.of(NEW_VARIANT, BLANK_VARIANT));

        assertEquals("Vợt Yonex", result.product().getName());
        assertEquals(new BigDecimal("1250000"), result.product().getPrice());
        assertEquals("racket.webp", result.product().getImagePath());
        assertEquals(1, result.variants().size());
        assertEquals("4U", result.variants().get(0).variant().getSizeLabel());
        assertEquals(6, result.variants().get(0).variant().getStock());
    }

    @Test
    void rejectsImagePathsOutsideAssetDirectory() {
        var unsafe = new CatalogBO.ProductFields("2", "Vợt", "Yonex", "", "1", "../racket.webp", true);

        assertThrows(CatalogBO.ValidationException.class,
                () -> CatalogBO.validateProduct(0, unsafe, List.of(NEW_VARIANT)));
    }

    @Test
    void rejectsPricesOutsideWholeVndAndDatabaseRange() {
        for (String price : List.of("12.50", "1000000000000", "0", "-1", "1e4")) {
            var invalid = new CatalogBO.ProductFields("2", "Vợt", "Yonex", "", price, "", true);
            assertThrows(CatalogBO.ValidationException.class,
                    () -> CatalogBO.validateProduct(0, invalid, List.of(NEW_VARIANT)), price);
        }
    }

    @Test
    void rejectsDuplicateSkusRegardlessOfCase() {
        var second = new CatalogBO.VariantFields("", "astrox-4u", "Xanh", "1", "", false);

        assertThrows(CatalogBO.ValidationException.class,
                () -> CatalogBO.validateProduct(0, PRODUCT, List.of(NEW_VARIANT, second)));
    }

    @Test
    void preservesExistingVariantIdAndOriginalStockWhenAdminChangesStock() throws Exception {
        var existing = new CatalogBO.VariantFields("42", "GRIP-01", "Hộp", "7", "4", false);
        var result = CatalogBO.validateProduct(5, PRODUCT, List.of(existing));

        assertEquals(42L, result.variants().get(0).variant().getId());
        assertEquals(7, result.variants().get(0).variant().getStock());
        assertEquals(4, result.variants().get(0).originalStock());
    }

    @Test
    void preservesBlankSizeForStandardVariants() throws Exception {
        var existing = new CatalogBO.VariantFields("42", "GRIP-01", "", "4", "4", false);
        var result = CatalogBO.validateProduct(5, PRODUCT, List.of(existing));

        assertEquals("", result.variants().get(0).variant().getSizeLabel());
    }

    @Test
    void requiresAtLeastOneVariantAfterRemovals() {
        var existing = new CatalogBO.VariantFields("42", "GRIP-01", "Hộp", "7", "4", true);

        assertThrows(CatalogBO.ValidationException.class,
                () -> CatalogBO.validateProduct(5, PRODUCT, List.of(existing, BLANK_VARIANT)));
    }

    @Test
    void treatsAnExistingVariantRemovalAsAnExplicitChange() throws Exception {
        var existing = new CatalogBO.VariantFields("42", "GRIP-01", "Hộp", "7", "4", true);
        var result = CatalogBO.validateProduct(5, PRODUCT, List.of(existing, NEW_VARIANT));

        assertTrue(result.variants().get(0).remove());
        assertEquals(42L, result.variants().get(0).variant().getId());
    }
}
