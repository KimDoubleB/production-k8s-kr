package com.example.demo.controller;

import java.util.List;

import com.example.demo.domain.todo.Todo;
import com.example.demo.dto.TodoRequest;
import com.example.demo.service.TodoService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;

	@GetMapping("/{id}")
	public Todo todo(@PathVariable final long id) {
		return todoService.getTodo(id);
	}

	@GetMapping
	public List<Todo> todoList() {
		return todoService.getTodoList();
	}

	@PostMapping
	public Todo newTodo(@RequestBody TodoRequest todoRequest) {
		return todoService.createTodo(todoRequest.content(), todoRequest.completed());
	}

	@PutMapping("/{id}")
	public Todo updateTodo(@PathVariable long id, @RequestBody TodoRequest todoRequest) {
		return todoService.updateTodo(id, todoRequest.content(), todoRequest.completed());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTodo(@PathVariable long id) {
		todoService.removeTodo(id);
	}
}
