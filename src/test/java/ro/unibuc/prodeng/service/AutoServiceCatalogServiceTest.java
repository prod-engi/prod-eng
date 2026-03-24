package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.CarEntity;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.model.MechanicEntity;
import ro.unibuc.prodeng.model.PartEntity;
import ro.unibuc.prodeng.model.SupplierEntity;
import ro.unibuc.prodeng.repository.CarRepository;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.MechanicRepository;
import ro.unibuc.prodeng.repository.PartRepository;
import ro.unibuc.prodeng.repository.SupplierRepository;
import ro.unibuc.prodeng.request.CreateCarRequest;
import ro.unibuc.prodeng.request.CreateClientRequest;
import ro.unibuc.prodeng.request.CreateMechanicRequest;
import ro.unibuc.prodeng.request.CreatePartRequest;
import ro.unibuc.prodeng.request.CreateSupplierRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AutoServiceCatalogServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private AutoServiceCatalogService catalogService;

    @Test
    void testCreateClient_newClientWithValidData_createsAndReturnsClient() {
        CreateClientRequest request = new CreateClientRequest("John", "Doe", "0700000000", "john@example.com", "Str. Main 1");
        ClientEntity saved = new ClientEntity("client-1", "John", "Doe", "0700000000", "john@example.com", "Str. Main 1");

        when(clientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(saved);

        ClientEntity result = catalogService.createClient(request);

        assertNotNull(result);
        assertEquals("client-1", result.id());
        assertEquals("John", result.firstName());
        assertEquals("john@example.com", result.email());
        verify(clientRepository, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void testCreateClient_duplicateEmail_throwsIllegalArgumentException() {
        CreateClientRequest request = new CreateClientRequest("John", "Doe", "0700000000", "john@example.com", "Str. Main 1");
        ClientEntity existing = new ClientEntity("client-1", "John", "Doe", "0700000000", "john@example.com", "Str. Main 1");

        when(clientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> catalogService.createClient(request));
    }

    @Test
    void testGetAllClients_withMultipleClients_returnsAllClients() {
        List<ClientEntity> clients = Arrays.asList(
                new ClientEntity("c-1", "Alice", "Smith", "0700000001", "alice@example.com", "Str. A"),
                new ClientEntity("c-2", "Bob", "Jones", "0700000002", "bob@example.com", "Str. B")
        );
        when(clientRepository.findAll()).thenReturn(clients);

        List<ClientEntity> result = catalogService.getAllClients();

        assertEquals(2, result.size());
    }

    @Test
    void testCreateCar_validRequest_createsAndReturnsCar() {
        CreateCarRequest request = new CreateCarRequest("Toyota", "Corolla", 2020, "B-123-XYZ", "client-1");
        ClientEntity client = new ClientEntity("client-1", "Alice", "Smith", "0700000001", "alice@example.com", "Str. A");
        CarEntity saved = new CarEntity("car-1", "Toyota", "Corolla", 2020, "B-123-XYZ", "client-1");

        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(carRepository.findByPlateNumber("B-123-XYZ")).thenReturn(Optional.empty());
        when(carRepository.save(any(CarEntity.class))).thenReturn(saved);

        CarEntity result = catalogService.createCar(request);

        assertNotNull(result);
        assertEquals("car-1", result.id());
        assertEquals("Toyota", result.brand());
        verify(carRepository, times(1)).save(any(CarEntity.class));
    }

    @Test
    void testCreateCar_clientNotFound_throwsEntityNotFoundException() {
        CreateCarRequest request = new CreateCarRequest("Toyota", "Corolla", 2020, "B-123-XYZ", "unknown-client");

        when(clientRepository.findById("unknown-client")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.createCar(request));
    }

    @Test
    void testCreateCar_duplicatePlateNumber_throwsIllegalArgumentException() {
        CreateCarRequest request = new CreateCarRequest("Toyota", "Corolla", 2020, "B-123-XYZ", "client-1");
        ClientEntity client = new ClientEntity("client-1", "Alice", "Smith", "0700000001", "alice@example.com", "Str. A");
        CarEntity existing = new CarEntity("car-1", "Honda", "Civic", 2019, "B-123-XYZ", "other-client");

        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(carRepository.findByPlateNumber("B-123-XYZ")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> catalogService.createCar(request));
    }

    @Test
    void testGetCarsByClientId_clientExists_returnsCars() {
        ClientEntity client = new ClientEntity("client-1", "Alice", "Smith", "0700000001", "alice@example.com", "Str. A");
        List<CarEntity> cars = Arrays.asList(
                new CarEntity("car-1", "Toyota", "Corolla", 2020, "B-111-AAA", "client-1")
        );

        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client));
        when(carRepository.findByClientId("client-1")).thenReturn(cars);

        List<CarEntity> result = catalogService.getCarsByClientId("client-1");

        assertEquals(1, result.size());
        assertEquals("car-1", result.get(0).id());
    }

    @Test
    void testCreateMechanic_validRequest_createsAndReturnsMechanic() {
        CreateMechanicRequest request = new CreateMechanicRequest("Ion", "Popescu", "0711111111");
        MechanicEntity saved = new MechanicEntity("mech-1", "Ion", "Popescu", "0711111111", 0.0, 0);

        when(mechanicRepository.save(any(MechanicEntity.class))).thenReturn(saved);

        MechanicEntity result = catalogService.createMechanic(request);

        assertNotNull(result);
        assertEquals("mech-1", result.id());
        assertEquals("Ion", result.firstName());
        assertEquals(0.0, result.score());
    }

    @Test
    void testGetAllMechanics_withMultipleMechanics_returnsAll() {
        List<MechanicEntity> mechanics = Arrays.asList(
                new MechanicEntity("m-1", "Ion", "Popescu", "0711111111", 4.5, 2),
                new MechanicEntity("m-2", "Ana", "Ionescu", "0722222222", 5.0, 1)
        );
        when(mechanicRepository.findAll()).thenReturn(mechanics);

        List<MechanicEntity> result = catalogService.getAllMechanics();

        assertEquals(2, result.size());
    }

    @Test
    void testCreateSupplier_validRequest_createsAndReturnsSupplier() {
        CreateSupplierRequest request = new CreateSupplierRequest("AutoParts SRL", "Str. Industriei 5", "0733333333");
        SupplierEntity saved = new SupplierEntity("sup-1", "AutoParts SRL", "Str. Industriei 5", "0733333333");

        when(supplierRepository.save(any(SupplierEntity.class))).thenReturn(saved);

        SupplierEntity result = catalogService.createSupplier(request);

        assertNotNull(result);
        assertEquals("sup-1", result.id());
        assertEquals("AutoParts SRL", result.name());
    }

    @Test
    void testGetAllSuppliers_withMultipleSuppliers_returnsAll() {
        List<SupplierEntity> suppliers = Arrays.asList(
                new SupplierEntity("s-1", "Supplier A", "Str. A", "0700000001"),
                new SupplierEntity("s-2", "Supplier B", "Str. B", "0700000002")
        );
        when(supplierRepository.findAll()).thenReturn(suppliers);

        List<SupplierEntity> result = catalogService.getAllSuppliers();

        assertEquals(2, result.size());
    }

    @Test
    void testCreatePart_validRequest_createsAndReturnsPart() {
        CreatePartRequest request = new CreatePartRequest("Spark Plug", 50, BigDecimal.valueOf(15), "sup-1");
        SupplierEntity supplier = new SupplierEntity("sup-1", "AutoParts SRL", "Str. Industriei 5", "0733333333");
        PartEntity saved = new PartEntity("part-1", "Spark Plug", 50, BigDecimal.valueOf(15), "sup-1");

        when(supplierRepository.findById("sup-1")).thenReturn(Optional.of(supplier));
        when(partRepository.save(any(PartEntity.class))).thenReturn(saved);

        PartEntity result = catalogService.createPart(request);

        assertNotNull(result);
        assertEquals("part-1", result.id());
        assertEquals("Spark Plug", result.name());
        assertEquals(50, result.availableStock());
    }

    @Test
    void testCreatePart_supplierNotFound_throwsEntityNotFoundException() {
        CreatePartRequest request = new CreatePartRequest("Spark Plug", 50, BigDecimal.valueOf(15), "unknown-sup");

        when(supplierRepository.findById("unknown-sup")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.createPart(request));
    }

    @Test
    void testGetAllParts_withMultipleParts_returnsAll() {
        List<PartEntity> parts = Arrays.asList(
                new PartEntity("p-1", "Oil Filter", 20, BigDecimal.valueOf(12), "sup-1"),
                new PartEntity("p-2", "Air Filter", 15, BigDecimal.valueOf(8), "sup-1")
        );
        when(partRepository.findAll()).thenReturn(parts);

        List<PartEntity> result = catalogService.getAllParts();

        assertEquals(2, result.size());
    }

    @Test
    void testEnsureCarExists_existingCar_returnsCar() {
        CarEntity car = new CarEntity("car-1", "Toyota", "Corolla", 2020, "B-123-XYZ", "client-1");
        when(carRepository.findById("car-1")).thenReturn(Optional.of(car));

        CarEntity result = catalogService.ensureCarExists("car-1");

        assertEquals("car-1", result.id());
    }

    @Test
    void testEnsureCarExists_nonExistingCar_throwsEntityNotFoundException() {
        when(carRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.ensureCarExists("missing"));
    }

    @Test
    void testEnsureMechanicExists_nonExistingMechanic_throwsEntityNotFoundException() {
        when(mechanicRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.ensureMechanicExists("missing"));
    }

    @Test
    void testEnsurePartExists_nonExistingPart_throwsEntityNotFoundException() {
        when(partRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.ensurePartExists("missing"));
    }

    @Test
    void testEnsureSupplierExists_nonExistingSupplier_throwsEntityNotFoundException() {
        when(supplierRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.ensureSupplierExists("missing"));
    }
}
