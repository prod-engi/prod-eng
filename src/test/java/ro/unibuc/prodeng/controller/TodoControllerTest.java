package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.AssignTodoRequest;
import ro.unibuc.prodeng.request.CreateTodoRequest;
import ro.unibuc.prodeng.request.EditTodoRequest;
import ro.unibuc.prodeng.response.TodoResponse;
import ro.unibuc.prodeng.service.TodoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    @InjectMocks
    private TodoController todoController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TodoResponse testTodo1 = new TodoResponse("todo-1", "Complete project", false, "Alice", "alice@example.com");
    private final TodoResponse testTodo2 = new TodoResponse("todo-2", "Write documentation", true, "Alice", "alice@example.com");
    private final CreateTodoRequest createTodoRequest = new CreateTodoRequest("Complete project", "alice@example.com");
    private final AssignTodoRequest assignTodoRequest = new AssignTodoRequest("bob@example.com");
    private final EditTodoRequest editTodoRequest = new EditTodoRequest("Updated description");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(todoController).build();
    }

    @Test
    void testGetTodosByUserEmail_withMultipleTodos_returnsList() throws Exception {
        List<TodoResponse> todos = Arrays.asList(testTodo1, testTodo2);
        when(todoService.getTodosByUserEmail("alice@example.com")).thenReturn(todos);

        mockMvc.perform(get("/api/todos")
                        .param("assigneeEmail", "alice@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("todo-1")))
                .andExpect(jsonPath("$[0].description", is("Complete project")))
                .andExpect(jsonPath("$[0].done", is(false)))
                .andExpect(jsonPath("$[1].id", is("todo-2")))
                .andExpect(jsonPath("$[1].description", is("Write documentation")))
                .andExpect(jsonPath("$[1].done", is(true)));

        verify(todoService, times(1)).getTodosByUserEmail("alice@example.com");
    }

    @Test
    void testGetTodosByUserEmail_withNoTodos_returnsEmptyList() throws Exception {
        when(todoService.getTodosByUserEmail("alice@example.com")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/todos")
                        .param("assigneeEmail", "alice@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(todoService, times(1)).getTodosByUserEmail("alice@example.com");
    }

    @Test
    void testGetTodoById_existingTodoRequested_returnsTodo() throws Exception {
        when(todoService.getTodoById("todo-1")).thenReturn(testTodo1);

        mockMvc.perform(get("/api/todos/{id}", "todo-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("todo-1")))
                .andExpect(jsonPath("$.description", is("Complete project")))
                .andExpect(jsonPath("$.done", is(false)))
                .andExpect(jsonPath("$.assigneeName", is("Alice")))
                .andExpect(jsonPath("$.assigneeEmail", is("alice@example.com")));

        verify(todoService, times(1)).getTodoById("todo-1");
    }

    @Test
    void testGetTodoById_nonExistingTodoRequested_returnsNotFound() throws Exception {
        when(todoService.getTodoById("missing")).thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(get("/api/todos/{id}", "missing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(todoService, times(1)).getTodoById("missing");
    }

    @Test
    void testCreateTodo_validRequestProvided_returnsCreatedTodo() throws Exception {
        when(todoService.createTodo(any(CreateTodoRequest.class))).thenReturn(testTodo1);

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTodoRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("todo-1")))
                .andExpect(jsonPath("$.description", is("Complete project")));

        verify(todoService, times(1)).createTodo(any(CreateTodoRequest.class));
    }

    @Test
    void testSetDone_existingTodoProvided_returnsUpdatedTodo() throws Exception {
        TodoResponse doneTodo = new TodoResponse("todo-1", "Complete project", true, "Alice", "alice@example.com");
        when(todoService.setDone(eq("todo-1"), eq(true))).thenReturn(doneTodo);

        mockMvc.perform(patch("/api/todos/{id}/done", "todo-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done", is(true)));

        verify(todoService, times(1)).setDone("todo-1", true);
    }

    @Test
    void testAssign_existingTodoWithNewAssignee_returnsReassignedTodo() throws Exception {
        TodoResponse reassigned = new TodoResponse("todo-1", "Complete project", false, "Bob", "bob@example.com");
        when(todoService.assign(eq("todo-1"), any(AssignTodoRequest.class))).thenReturn(reassigned);

        mockMvc.perform(patch("/api/todos/{id}/assignee", "todo-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignTodoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName", is("Bob")))
                .andExpect(jsonPath("$.assigneeEmail", is("bob@example.com")));

        verify(todoService, times(1)).assign(eq("todo-1"), any(AssignTodoRequest.class));
    }

    @Test
    void testEdit_existingTodoWithNewDescription_returnsUpdatedTodo() throws Exception {
        TodoResponse edited = new TodoResponse("todo-1", "Updated description", false, "Alice", "alice@example.com");
        when(todoService.edit(eq("todo-1"), any(EditTodoRequest.class))).thenReturn(edited);

        mockMvc.perform(patch("/api/todos/{id}/description", "todo-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editTodoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated description")));

        verify(todoService, times(1)).edit(eq("todo-1"), any(EditTodoRequest.class));
    }

    @Test
    void testDeleteTodo_existingTodoRequested_returnsNoContent() throws Exception {
        doNothing().when(todoService).deleteTodo("todo-1");

        mockMvc.perform(delete("/api/todos/{id}", "todo-1"))
                .andExpect(status().isNoContent());

        verify(todoService, times(1)).deleteTodo("todo-1");
    }

    @Test
    void testDeleteTodo_nonExistingTodoRequested_returnsNotFound() throws Exception {
        doThrow(new EntityNotFoundException("missing")).when(todoService).deleteTodo("missing");

        mockMvc.perform(delete("/api/todos/{id}", "missing"))
                .andExpect(status().isNotFound());

        verify(todoService, times(1)).deleteTodo("missing");
    }
}
