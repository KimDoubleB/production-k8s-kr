package com.example.demo.service;

import java.util.List;

import com.example.demo.aop.BooleanFlag;
import com.example.demo.domain.todo.Todo;
import com.example.demo.domain.todo.TodoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

	private final TodoRepository todoRepository;

	@Override
	@Transactional
	@BooleanFlag("FLAG_CREATE_TODO")
	public Todo createTodo(String content, boolean completed) {
		final Todo newTodo = Todo.builder()
				.content(content)
				.completed(completed)
				.build();

		return todoRepository.save(newTodo);
	}

	@Override
	public List<Todo> getTodoList() {
		return todoRepository.findAll();
	}

	@Override
	public Todo getTodo(long id) {
		return todoRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("해당하는 데이터가 없습니다."));
	}

	@Override
	@Transactional
	public Todo updateTodo(long id, String content, boolean completed) {
		final Todo oldTodo = getTodo(id);
		oldTodo.updateContent(content);
		oldTodo.updateCompleted(completed);

		return todoRepository.save(oldTodo);
	}

	@Override
	@Transactional
	public void removeTodo(long id) {
		todoRepository.deleteById(id);
	}
}
