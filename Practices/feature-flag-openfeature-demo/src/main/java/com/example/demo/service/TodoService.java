package com.example.demo.service;

import java.util.List;

import com.example.demo.domain.todo.Todo;

public interface TodoService {

	Todo createTodo(String content, boolean completed);
	List<Todo> getTodoList();
	Todo getTodo(long id);

	Todo updateTodo(long id, String content, boolean completed);

	void removeTodo(long id);
}
