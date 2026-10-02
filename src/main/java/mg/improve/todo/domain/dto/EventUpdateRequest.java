package mg.improve.todo.domain.dto;

import java.time.Instant;

public class EventUpdateRequest {

	private String title;

	private String description;

	private Instant startTime;

	private Instant endTime;

	private boolean titlePresent;

	private boolean descriptionPresent;

	private boolean startTimePresent;

	private boolean endTimePresent;

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

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
		this.startTimePresent = true;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
		this.endTimePresent = true;
	}

	public boolean isTitlePresent() {
		return titlePresent;
	}

	public boolean isDescriptionPresent() {
		return descriptionPresent;
	}

	public boolean isStartTimePresent() {
		return startTimePresent;
	}

	public boolean isEndTimePresent() {
		return endTimePresent;
	}
}
