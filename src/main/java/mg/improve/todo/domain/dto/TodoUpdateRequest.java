package mg.improve.todo.domain.dto;

import java.time.Instant;

public class TodoUpdateRequest {

	private String title;

	private String description;

	private Boolean done;

	private Instant dueDate;

	private boolean titlePresent;

	private boolean descriptionPresent;

	private boolean donePresent;

	private boolean dueDatePresent;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		this.titlePresent = true;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.descriptionPresent = true;
	}

	public Boolean getDone() {
		return done;
	}

	public void setDone(Boolean done) {
		this.done = done;
		this.donePresent = true;
	}

	public Instant getDueDate() {
		return dueDate;
	}

	public void setDueDate(Instant dueDate) {
		this.dueDate = dueDate;
		this.dueDatePresent = true;
	}

	public boolean isTitlePresent() {
		return titlePresent;
	}

	public boolean isDescriptionPresent() {
		return descriptionPresent;
	}

	public boolean isDonePresent() {
		return donePresent;
	}

	public boolean isDueDatePresent() {
		return dueDatePresent;
	}
}
