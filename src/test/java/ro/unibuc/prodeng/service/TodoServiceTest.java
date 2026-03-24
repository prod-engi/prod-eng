package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.TodoEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.TodoRepository;
import ro.unibuc.prodeng.request.AssignTodoRequest;
import ro.unibuc.prodeng.request.CreateTodoRequest;
import ro.unibuc.prodeng.request.EditTodoRequest;
import ro.unibuc.prodeng.response.TodoResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TodoService todoService;

    @Test
    void testGetTodosByUserEmail_userWithTodos_returnsAllTodos() throws EntityNotFoundException {
        UserEntity user = new UserEntity("1", "Alice", "alice@example.com");
        List<TodoEntity> todos = Arrays.asList(
                new TodoEntity("todo-1", "Buy milk", false, "1"),
                new TodoEntity("todo-2", "Walk dog", true, "1")
        );

        when(userService.getUserEntityByEmail("alice@example.com")).thenReturn(user);
        when(todoRepository.findByAssignedUserId("1")).thenReturn(todos);

        List<TodoResponse> result = todoService.getTodosByUserEmail("alice@example.com");

        assertEquals(2, result.size());
        assertEquals("Buy milk", result.get(0).description());
        assertEquals("Walk dog", result.get(1).description());
        verify(todoRepository, times(1)).findByAssignedUserId("1");
    }

    @Test
    void testGetTodosByUserEmail_userNotFound_throwsEntityNotFoundException() throws EntityNotFoundException {
        when(userService.getUserEntityByEmail("unknown@example.com"))
                .thenThrow(new EntityNotFoundException("unknown@example.com"));

        assertThrows(EntityNotFoundException.class,
                () -> todoService.getTodosByUserEmail("unknown@example.com"));
    }

    @Test
    void testGetTodoById_existingTodoRequested_returnsTodo() throws EntityNotFoundException {
        UserEntity user = new UserEntity("1", "Alice", "alice@example.com");
        TodoEntity todo = new TodoEntity("todo-1", "Complete project", false, "1");

        when(todoRepository.findById("todo-1")).thenReturn(Optional.of(todo));
        when(userService.getUserEntityById("1")).thenReturn(user);

        TodoResponse result = todoService.getTodoById("todo-1");

        assertNotNull(result);
        assertEquals("todo-1", result.id());
        assertEquals("Complete project", result.description());
        assertFalse(result.done());
        assertEquals("Alice", result.assigneeName());
        assertEquals("alice@example.com", result.assigneeEmail());
    }

    @Test
    void testGetTodoById_nonExistingTodoRequested_throwsEntityNotFoundException() {
        when(todoRepository.findById("non-existing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> todoService.getTodoById("non-existing"));
    }

    @Test
    void testCreateTodo_newTodoWithValidData_createsAndReturnsTodo() throws EntityNotFoundException {
        UserEntity assignee = new UserEntity("1", "Alice", "alice@example.com");
        CreateTodoRequest request = new CreateTodoRequest("Finish unit tests", "alice@example.com");

        when(userService.getUserEntityByEmail("alice@example.com")).thenReturn(assignee);
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> {
            TodoEntity entity = invocation.getArgument(0);
            return new TodoEntity("generated-todo-id-123", entity.description(), entity.done(), entity.assignedUserId());
        });

        TodoResponse result = todoService.createTodo(request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Finish unit tests", result.description());
        assertFalse(result.done());
        assertEquals("Alice", result.assigneeName());
        assertEquals("alice@example.com", result.assigneeEmail());
        verify(todoRepository, times(1)).save(any(TodoEntity.class));
    }

    @Test
    void testCreateTodo_assigneeNotFound_throwsEntityNotFoundException() throws EntityNotFoundException {
        CreateTodoRequest request = new CreateTodoRequest("Some task", "ghost@example.com");

        when(userService.getUserEntityByEmail("ghost@example.com"))
                .thenThrow(new EntityNotFoundException("ghost@example.com"));

        assertThrows(EntityNotFoundException.class, () -> todoService.createTodo(request));
    }

    @Test
    void testSetDone_existingTodoMarkedDone_returnsUpdatedTodo() throws EntityNotFoundException {
        UserEntity user = new UserEntity("1", "Alice", "alice@example.com");
        TodoEntity existing = new TodoEntity("todo-1", "Read book", false, "1");
        TodoEntity updated = new TodoEntity("todo-1", "Read book", true, "1");

        when(todoRepository.findById("todo-1")).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updated);
        when(userService.getUserEntityById("1")).thenReturn(user);

        TodoResponse result = todoService.setDone("todo-1", true);

        assertTrue(result.done());
        assertEquals("Read book", result.description());
    }

    @Test
    void testSetDone_nonExistingTodo_throwsEntityNotFoundException() {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> todoService.setDone("missing", true));
    }

    @Test
    void testAssign_existingTodoWithNewAssignee_returnsReassignedTodo() throws EntityNotFoundException {
        UserEntity newAssignee = new UserEntity("2", "Bob", "bob@example.com");
        TodoEntity existing = new TodoEntity("todo-1", "Fix bug", false, "1");
        TodoEntity reassigned = new TodoEntity("todo-1", "Fix bug", false, "2");
        AssignTodoRequest request = new AssignTodoRequest("bob@example.com");

        when(todoRepository.findById("todo-1")).thenReturn(Optional.of(existing));
        when(userService.getUserEntityByEmail("bob@example.com")).thenReturn(newAssignee);
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(reassigned);

        TodoResponse result = todoService.assign("todo-1", request);

        assertEquals("Bob", result.assigneeName());
        assertEquals("bob@example.com", result.assigneeEmail());
    }

    @Test
    void testAssign_nonExistingTodo_throwsEntityNotFoundException() {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> todoService.assign("missing", new AssignTodoRequest("bob@example.com")));
    }

    @Test
    void testEdit_existingTodoWithNewDescription_returnsUpdatedTodo() throws EntityNotFoundException {
        UserEntity user = new UserEntity("1", "Alice", "alice@example.com");
        TodoEntity existing = new TodoEntity("todo-1", "Old description", false, "1");
        TodoEntity updated = new TodoEntity("todo-1", "New description", false, "1");
        EditTodoRequest request = new EditTodoRequest("New description");

        when(todoRepository.findById("todo-1")).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoEntity.class))).thenReturn(updated);
        when(userService.getUserEntityById("1")).thenReturn(user);

        TodoResponse result = todoService.edit("todo-1", request);

        assertEquals("New description", result.description());
    }

    @Test
    void testEdit_nonExistingTodo_throwsEntityNotFoundException() {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> todoService.edit("missing", new EditTodoRequest("New description")));
    }

    @Test
    void testDeleteTodo_existingTodo_deletesSuccessfully() throws EntityNotFoundException {
        when(todoRepository.existsById("todo-1")).thenReturn(true);

        todoService.deleteTodo("todo-1");

        verify(todoRepository, times(1)).deleteById("todo-1");
    }

    @Test
    void testDeleteTodo_nonExistingTodo_throwsEntityNotFoundException() {
        when(todoRepository.existsById("missing")).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> todoService.deleteTodo("missing"));
    }
}
