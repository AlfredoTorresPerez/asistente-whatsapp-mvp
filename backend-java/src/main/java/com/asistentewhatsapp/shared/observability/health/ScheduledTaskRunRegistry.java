package com.asistentewhatsapp.shared.observability.health;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTaskRunRegistry {

	private final ConcurrentMap<String, TaskRun> runs = new ConcurrentHashMap<>();

	public void markSuccess(String task) {
		runs.put(task, new TaskRun(Instant.now(), true));
	}

	public void markFailure(String task) {
		runs.put(task, new TaskRun(Instant.now(), false));
	}

	public Instant lastRun(String task) {
		TaskRun run = runs.get(task);
		return run == null ? null : run.timestamp();
	}

	public boolean lastSuccessful(String task) {
		TaskRun run = runs.get(task);
		return run == null || run.successful();
	}

	public java.util.Collection<TaskRun> allRuns() {
		return runs.values();
	}

	public record TaskRun(Instant timestamp, boolean successful) {
	}
}
