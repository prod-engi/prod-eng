package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.request.CreateServiceOrderRequest;
import ro.unibuc.prodeng.request.RequiredPartRequest;
import ro.unibuc.prodeng.response.ServiceOrderResponse;
import ro.unibuc.prodeng.service.ServiceOrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class ServiceOrderControllerTest {

    @Mock
    private ServiceOrderService serviceOrderService;

    @InjectMocks
    private ServiceOrderController serviceOrderController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ServiceOrderResponse inProgressOrder = new ServiceOrderResponse(
            "order-1", "car-1", "mech-1", "Oil Change", "Replace oil",
            BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(150),
            List.of(), LocalDateTime.of(2026, 1, 1, 10, 0), null, OrderStatus.IN_PROGRESS
    );

    private final ServiceOrderResponse completedOrder = new ServiceOrderResponse(
            "order-1", "car-1", "mech-1", "Oil Change", "Replace oil",
            BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(150),
            List.of(), LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 2, 10, 0), OrderStatus.COMPLETED
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(serviceOrderController).build();
    }

    @Test
    void testCreateServiceOrder_validRequestProvided_returnsCreatedOrder() throws Exception {
        CreateServiceOrderRequest request = new CreateServiceOrderRequest(
                "car-1", "mech-1", "Oil Change", "Replace oil",
                BigDecimal.valueOf(100), LocalDateTime.of(2026, 1, 1, 10, 0),
                List.of(new RequiredPartRequest("part-1", 2))
        );

        when(serviceOrderService.createServiceOrder(any(CreateServiceOrderRequest.class))).thenReturn(inProgressOrder);

        mockMvc.perform(post("/api/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("order-1")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        verify(serviceOrderService, times(1)).createServiceOrder(any(CreateServiceOrderRequest.class));
    }

    @Test
    void testCompleteOrder_existingOrderProvided_returnsCompletedOrder() throws Exception {
        when(serviceOrderService.completeOrder("order-1")).thenReturn(completedOrder);

        mockMvc.perform(patch("/api/service-orders/{orderId}/complete", "order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("order-1")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(serviceOrderService, times(1)).completeOrder("order-1");
    }

    @Test
    void testGetOrderById_existingOrderRequested_returnsOrder() throws Exception {
        when(serviceOrderService.getOrderById("order-1")).thenReturn(inProgressOrder);

        mockMvc.perform(get("/api/service-orders/{orderId}", "order-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("order-1")))
                .andExpect(jsonPath("$.carId", is("car-1")))
                .andExpect(jsonPath("$.mechanicId", is("mech-1")));

        verify(serviceOrderService, times(1)).getOrderById("order-1");
    }

    @Test
    void testGetOrderById_nonExistingOrderRequested_returnsNotFound() throws Exception {
        when(serviceOrderService.getOrderById("missing"))
                .thenThrow(new EntityNotFoundException("ServiceOrder missing"));

        mockMvc.perform(get("/api/service-orders/{orderId}", "missing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(serviceOrderService, times(1)).getOrderById("missing");
    }

    @Test
    void testGetOrdersByStatus_withInProgressStatus_returnsMatchingOrders() throws Exception {
        List<ServiceOrderResponse> orders = Arrays.asList(inProgressOrder);
        when(serviceOrderService.getOrdersByStatus(OrderStatus.IN_PROGRESS)).thenReturn(orders);

        mockMvc.perform(get("/api/service-orders")
                        .param("status", "IN_PROGRESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("order-1")))
                .andExpect(jsonPath("$[0].status", is("IN_PROGRESS")));

        verify(serviceOrderService, times(1)).getOrdersByStatus(OrderStatus.IN_PROGRESS);
    }
}
