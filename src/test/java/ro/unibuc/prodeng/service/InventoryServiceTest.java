package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.PartEntity;
import ro.unibuc.prodeng.repository.PartRepository;
import ro.unibuc.prodeng.request.ReceiveDeliveryRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class InventoryServiceTest {

    @Mock
    private AutoServiceCatalogService catalogService;

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void testReceiveDelivery_validRequest_updatesStockAndReturnsPart() {
        PartEntity existingPart = new PartEntity("part-1", "Oil Filter", 10, BigDecimal.valueOf(20), "sup-1");
        PartEntity updatedPart = new PartEntity("part-1", "Oil Filter", 15, BigDecimal.valueOf(20), "sup-1");
        ReceiveDeliveryRequest request = new ReceiveDeliveryRequest("part-1", "sup-1", 5, LocalDateTime.now());

        when(catalogService.ensurePartExists("part-1")).thenReturn(existingPart);
        when(partRepository.save(any(PartEntity.class))).thenReturn(updatedPart);

        PartEntity result = inventoryService.receiveDelivery(request);

        assertNotNull(result);
        assertEquals(15, result.availableStock());
        verify(partRepository, times(1)).save(any(PartEntity.class));
    }

    @Test
    void testReceiveDelivery_wrongSupplier_throwsIllegalArgumentException() {
        PartEntity part = new PartEntity("part-1", "Oil Filter", 10, BigDecimal.valueOf(20), "sup-1");
        ReceiveDeliveryRequest request = new ReceiveDeliveryRequest("part-1", "wrong-sup", 5, LocalDateTime.now());

        when(catalogService.ensurePartExists("part-1")).thenReturn(part);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.receiveDelivery(request));
        verify(partRepository, never()).save(any());
    }

    @Test
    void testReceiveDelivery_partNotFound_throwsEntityNotFoundException() {
        ReceiveDeliveryRequest request = new ReceiveDeliveryRequest("missing-part", "sup-1", 5, LocalDateTime.now());

        when(catalogService.ensurePartExists("missing-part"))
                .thenThrow(new EntityNotFoundException("Part missing-part"));

        assertThrows(EntityNotFoundException.class, () -> inventoryService.receiveDelivery(request));
    }
}
